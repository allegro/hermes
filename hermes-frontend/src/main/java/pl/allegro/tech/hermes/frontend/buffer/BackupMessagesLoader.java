package pl.allegro.tech.hermes.frontend.buffer;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.avro.Schema;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.BrokerTopicAvailabilityChecker;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.avro.AvroMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaExistenceEnsurer;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersion;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

public class BackupMessagesLoader {

  private static final Logger logger = LoggerFactory.getLogger(BackupMessagesLoader.class);

  private final BrokerTopicAvailabilityChecker brokerTopicAvailabilityChecker;
  private final BrokerMessageProducer brokerMessageProducer;
  private final BrokerListeners brokerListeners;
  private final TopicsCache topicsCache;
  private final SchemaRepository schemaRepository;
  private final SchemaExistenceEnsurer schemaExistenceEnsurer;
  private final Trackers trackers;
  private final Duration messageMaxAgeHours;
  private final int maxResendRetries;
  private final Duration resendSleep;
  private final Duration readTopicInfoSleep;

  private final Set<Topic> topicsAvailabilityCache = new HashSet<>();
  private final AtomicReference<ConcurrentLinkedQueue<Pair<Message, CachedTopic>>> toResend =
      new AtomicReference<>();

  public BackupMessagesLoader(
      BrokerTopicAvailabilityChecker brokerTopicAvailabilityChecker,
      BrokerMessageProducer brokerMessageProducer,
      BrokerListeners brokerListeners,
      TopicsCache topicsCache,
      SchemaRepository schemaRepository,
      SchemaExistenceEnsurer schemaExistenceEnsurer,
      Trackers trackers,
      BackupMessagesLoaderParameters backupMessagesLoaderParameters) {
    this.brokerTopicAvailabilityChecker = brokerTopicAvailabilityChecker;
    this.brokerMessageProducer = brokerMessageProducer;
    this.brokerListeners = brokerListeners;
    this.topicsCache = topicsCache;
    this.schemaRepository = schemaRepository;
    this.schemaExistenceEnsurer = schemaExistenceEnsurer;
    this.trackers = trackers;
    this.messageMaxAgeHours = backupMessagesLoaderParameters.getMaxAge();
    this.resendSleep = backupMessagesLoaderParameters.getLoadingPauseBetweenResend();
    this.readTopicInfoSleep = backupMessagesLoaderParameters.getLoadingWaitForBrokerTopicInfo();
    this.maxResendRetries = backupMessagesLoaderParameters.getMaxResendRetries();
  }

  public void loadMessages(List<BackupMessage> messages) {
    logger.info("Loading {} messages from backup storage.", messages.size());
    toResend.set(new ConcurrentLinkedQueue<>());

    sendMessages(messages);

    if (toResend.get().size() == 0) {
      logger.info("No messages to resend.");
      return;
    }

    int retry = 0;
    do {
      if (retry > 0) {
        List<Pair<Message, CachedTopic>> retryMessages =
            Lists.newArrayList(toResend.getAndSet(new ConcurrentLinkedQueue<>()));
        resendMessages(retryMessages, retry);
      }
      try {
        Thread.sleep(resendSleep.toMillis());
      } catch (InterruptedException e) {
        logger.warn("Sleep interrupted", e);
      }
      retry++;
    } while (toResend.get().size() > 0 && retry <= maxResendRetries);

    logger.info(
        "Finished resending messages from backup storage after retry #{} with {} unsent messages.",
        retry - 1,
        toResend.get().size());
  }

  public void loadFromTemporaryBackupV2File(File file) {
    try (FileInputStream fileInputStream = new FileInputStream(file);
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
      List<BackupMessage> messages = (List<BackupMessage>) objectInputStream.readObject();
      logger.info("Loaded {} messages from temporary v2 backup file: {}", messages.size(), file);
      loadMessages(messages);

    } catch (IOException | ClassNotFoundException e) {
      logger.error(
          "Error reading temporary backup v2 files from path {}.", file.getAbsolutePath(), e);
    }
  }

  public void clearTopicsAvailabilityCache() {
    topicsAvailabilityCache.clear();
  }

  private void sendMessages(List<BackupMessage> messages) {
    logger.info("Sending {} messages from backup storage.", messages.size());
    int sentCounter = 0;
    int discardedCounter = 0;
    for (BackupMessage backupMessage : messages) {
      String topicQualifiedName = backupMessage.getQualifiedTopicName();
      Optional<CachedTopic> optionalCachedTopic = topicsCache.getTopic(topicQualifiedName);
      if (sendBackupMessageIfNeeded(
          backupMessage, topicQualifiedName, optionalCachedTopic, "sending")) {
        sentCounter++;
      } else {
        discardedCounter++;
      }
    }
    logger.info(
        "Loaded and sent {} messages and discarded {} messages from the backup storage.",
        sentCounter,
        discardedCounter);
  }

  private void resendMessages(List<Pair<Message, CachedTopic>> messageAndTopicList, int retry) {
    logger.info(
        "Resending {} messages from backup storage retry {}.", messageAndTopicList.size(), retry);

    int sentCounter = 0;
    int discardedCounter = 0;
    for (Pair<Message, CachedTopic> messageAndTopic : messageAndTopicList) {
      Message message = messageAndTopic.getKey();
      Optional<CachedTopic> cachedTopic = Optional.of(messageAndTopic.getValue());
      if (sendMessageIfNeeded(
          message, cachedTopic.get().getQualifiedName(), cachedTopic, "resending")) {
        sentCounter++;
      } else {
        discardedCounter++;
      }
    }

    logger.info(
        "Resent {}/{} messages and discarded {} messages from the backup storage retry {}.",
        sentCounter,
        messageAndTopicList.size(),
        discardedCounter,
        retry);
  }

  private boolean sendBackupMessageIfNeeded(
      BackupMessage backupMessage,
      String topicQualifiedName,
      Optional<CachedTopic> cachedTopic,
      String contextName) {
    if (cachedTopic.isPresent()) {
      Message message;
      if (backupMessage.getSchemaVersion() != null) {
        message = createAvroMessageFromVersion(backupMessage, cachedTopic);
      } else if (backupMessage.getSchemaId() != null) {
        message = createAvroMessageFromSchemaId(backupMessage, cachedTopic);
      } else {
        message = createJsonMessage(backupMessage);
      }
      return sendMessageIfNeeded(message, topicQualifiedName, cachedTopic, contextName);
    }
    return false;
  }

  private Message createAvroMessageFromSchemaId(
      BackupMessage backupMessage, Optional<CachedTopic> cachedTopic) {
    SchemaId schemaId = SchemaId.valueOf(backupMessage.getSchemaId());
    schemaExistenceEnsurer.ensureSchemaExists(cachedTopic.get().getTopic(), schemaId);
    CompiledSchema<Schema> schema =
        schemaRepository.getAvroSchema(cachedTopic.get().getTopic(), schemaId);
    return createAvroMessage(backupMessage, schema);
  }

  private Message createAvroMessageFromVersion(
      BackupMessage backupMessage, Optional<CachedTopic> cachedTopic) {
    SchemaVersion version = SchemaVersion.valueOf(backupMessage.getSchemaVersion());
    schemaExistenceEnsurer.ensureSchemaExists(cachedTopic.get().getTopic(), version);
    CompiledSchema<Schema> schema =
        schemaRepository.getAvroSchema(cachedTopic.get().getTopic(), version);
    return createAvroMessage(backupMessage, schema);
  }

  private Message createAvroMessage(BackupMessage backupMessage, CompiledSchema<Schema> schema) {
    return new AvroMessage(
        backupMessage.getMessageId(),
        backupMessage.getData(),
        backupMessage.getTimestamp(),
        schema,
        backupMessage.getPartitionKey(),
        backupMessage.getPropagatedHTTPHeaders());
  }

  private Message createJsonMessage(BackupMessage backupMessage) {
    return new JsonMessage(
        backupMessage.getMessageId(),
        backupMessage.getData(),
        backupMessage.getTimestamp(),
        backupMessage.getPartitionKey(),
        backupMessage.getPropagatedHTTPHeaders());
  }

  private boolean sendMessageIfNeeded(
      Message message,
      String topicQualifiedName,
      Optional<CachedTopic> cachedTopic,
      String contextName) {
    if (cachedTopic.isPresent()) {
      if (isNotStale(message)) {
        waitOnBrokerTopicAvailability(cachedTopic.get());
        sendMessage(message, cachedTopic.get());
        return true;
      }
      logger.warn(
          "Not {} stale message {} {} {}",
          contextName,
          message.getId(),
          topicQualifiedName,
          new String(message.getData(), Charset.defaultCharset()));
      return false;
    }
    logger.error(
        "Topic {} not present. Not {} message {} {}",
        topicQualifiedName,
        contextName,
        message.getId(),
        new String(message.getData(), Charset.defaultCharset()));
    return false;
  }

  private void waitOnBrokerTopicAvailability(CachedTopic cachedTopic) {
    int tries = 0;
    while (!isBrokerTopicAvailable(cachedTopic)) {
      try {
        tries++;
        logger.info(
            "Broker topic {} is not available, checked {} times.",
            cachedTopic.getTopic().getQualifiedName(),
            tries);
        Thread.sleep(readTopicInfoSleep.toMillis());
      } catch (InterruptedException e) {
        logger.warn(
            "Waiting for broker topic availability interrupted. Topic: {}",
            cachedTopic.getTopic().getQualifiedName());
      }
    }
  }

  private boolean isBrokerTopicAvailable(CachedTopic cachedTopic) {
    if (topicsAvailabilityCache.contains(cachedTopic.getTopic())) {
      return true;
    }

    if (brokerTopicAvailabilityChecker.isTopicAvailable(cachedTopic)) {
      topicsAvailabilityCache.add(cachedTopic.getTopic());
      logger.info("Broker topic {} is available.", cachedTopic.getTopic().getQualifiedName());
      return true;
    }

    return false;
  }

  private boolean isNotStale(Message message) {
    return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(message.getTimestamp()), ZoneId.systemDefault())
        .isAfter(LocalDateTime.now().minusHours(messageMaxAgeHours.toHours()));
  }

  private void sendMessage(Message message, CachedTopic cachedTopic) {
    brokerMessageProducer.send(
        message,
        cachedTopic,
        new PublishingCallback() {
          @Override
          public void onUnpublished(Message message, Topic topic, Exception exception) {
            brokerListeners.onError(message, topic, exception);
            trackers
                .get(topic)
                .logError(
                    message.getId(),
                    topic.getName(),
                    exception.getMessage(),
                    "",
                    Collections.emptyMap());
            toResend.get().add(ImmutablePair.of(message, cachedTopic));
          }

          @Override
          public void onPublished(Message message, Topic topic) {
            brokerListeners.onAcknowledge(message, topic);
          }

          @Override
          public void onEachPublished(Message message, Topic topic, String datacenter) {
            cachedTopic.incrementPublished(datacenter);
            trackers
                .get(topic)
                .logPublished(
                    message.getId(), topic.getName(), "", datacenter, Collections.emptyMap());
          }
        });
  }
}
