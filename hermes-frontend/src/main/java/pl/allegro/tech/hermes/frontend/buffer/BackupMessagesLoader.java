package pl.allegro.tech.hermes.frontend.buffer;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.metric.StartedTimersPair;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static pl.allegro.tech.hermes.common.config.Configs.*;

public class BackupMessagesLoader {

    private static final Logger logger = LoggerFactory.getLogger(BackupMessagesLoader.class);

    private final BrokerMessageProducer brokerMessageProducer;
    private final BrokerListeners brokerListeners;
    private final TopicsCache topicsCache;
    private final Trackers trackers;
    private final int messageMaxAgeHours;
    private final int maxResendRetries;
    private final long resendSleep;
    private final long readTopicInfoSleep;

    private final Set<Topic> topicsAvailabilityCache = new HashSet<>();
    private final AtomicReference<ConcurrentLinkedQueue<Pair<Message, CachedTopic>>> toResend = new AtomicReference<>();

    @Inject
    public BackupMessagesLoader(BrokerMessageProducer brokerMessageProducer,
                                BrokerListeners brokerListeners,
                                TopicsCache topicsCache,
                                Trackers trackers,
                                ConfigFactory config) {
        this.brokerMessageProducer = brokerMessageProducer;
        this.brokerListeners = brokerListeners;
        this.topicsCache = topicsCache;
        this.trackers = trackers;
        this.messageMaxAgeHours = config.getIntProperty(MESSAGES_LOCAL_STORAGE_MAX_AGE_HOURS);
        this.resendSleep = config.getIntProperty(MESSAGES_LOADING_PAUSE_BETWEEN_RESENDS);
        this.readTopicInfoSleep = TimeUnit.SECONDS.toMillis(config.getIntProperty(MESSAGES_LOADING_WAIT_FOR_BROKER_TOPIC_INFO));
        this.maxResendRetries = config.getIntProperty(MESSAGES_LOCAL_STORAGE_MAX_RESEND_RETRIES);
    }

    public void loadMessages(MessageRepository messageRepository) {
        List<BackupMessage> messages = messageRepository.findAll();
        logger.info("Loading {} messages from backup storage.", messages.size());
        int retry = 0;
        toResend.set(new ConcurrentLinkedQueue<>());

        sendMessages(messages);

        if (toResend.get().size() == 0) {
            logger.info("No messages to resend.");
            return;
        }

        do {
            if (retry > 0) {
                List<Pair<Message, CachedTopic>> retryMessages = Lists.newArrayList(toResend.getAndSet(new ConcurrentLinkedQueue<>()));
                resendMessages(retryMessages, retry);
            }
            try {
                Thread.sleep(resendSleep);
            } catch (InterruptedException e) {
                logger.warn("Sleep interrupted", e);
            }
            retry++;
        } while (toResend.get().size() > 0 && retry <= maxResendRetries);

        logger.info("Finished resending messages from backup storage after retry #{} with {} unsent messages.", retry - 1, toResend.get().size());
    }

    public void clearTopicsAvailabilityCache() {
        topicsAvailabilityCache.clear();
    }

    private void sendMessages(List<BackupMessage> messages) {
        logger.info("Sending {} messages from backup storage.", messages.size());
        int sentCounter = 0;
        int discardedCounter = 0;
        for (BackupMessage backupMessage : messages) {
            Message message = new JsonMessage(backupMessage.getMessageId(), backupMessage.getData(), backupMessage.getTimestamp());
            String topicQualifiedName = backupMessage.getQualifiedTopicName();
            Optional<CachedTopic> optionalCachedTopic = topicsCache.getTopic(topicQualifiedName);
            if (sendMessageIfNeeded(message, topicQualifiedName, optionalCachedTopic, "sending")) {
                sentCounter++;
            } else {
                discardedCounter++;
            }
        }
        logger.info("Loaded and sent {} messages and discarded {} messages from the backup storage.", sentCounter, discardedCounter);
    }

    private void resendMessages(List<Pair<Message, CachedTopic>> messageAndTopicList, int retry) {
        logger.info("Resending {} messages from backup storage retry {}.", messageAndTopicList.size(), retry);

        int sentCounter = 0;
        int discardedCounter = 0;
        for (Pair<Message, CachedTopic> messageAndTopic : messageAndTopicList) {
            Message message = messageAndTopic.getKey();
            Optional<CachedTopic> cachedTopic = Optional.of(messageAndTopic.getValue());
            if (sendMessageIfNeeded(message, cachedTopic.get().getQualifiedName(), cachedTopic, "resending")) {
                sentCounter++;
            } else {
                discardedCounter++;
            }
        }

        logger.info("Resent {}/{} messages and discarded {} messages from the backup storage retry {}.", sentCounter, messageAndTopicList.size(), discardedCounter, retry);
    }

    private boolean sendMessageIfNeeded(Message message, String topicQualifiedName, Optional<CachedTopic> cachedTopic, String contextName) {
        if (cachedTopic.isPresent()) {
            if (isNotStale(message)) {
                waitOnBrokerTopicAvailability(cachedTopic.get());
                sendMessage(message, cachedTopic.get());
                return true;
            }
            logger.warn("Not {} stale message {} {} {}", contextName, message.getId(),
                    topicQualifiedName, new String(message.getData(), Charset.defaultCharset()));
            return false;
        }
        logger.error("Topic {} not present. Not {} message {} {}", topicQualifiedName, contextName,
                message.getId(), new String(message.getData(), Charset.defaultCharset()));
        return false;
    }

    private void waitOnBrokerTopicAvailability(CachedTopic cachedTopic) {
        int tries = 0;
        while(!isBrokerTopicAvailable(cachedTopic)) {
            try {
                tries++;
                logger.info("Broker topic {} is not available, checked {} times.", cachedTopic.getTopic().getQualifiedName(), tries);
                Thread.sleep(readTopicInfoSleep);
            } catch (InterruptedException e) {
                logger.warn("Waiting for broker topic availability interrupted. Topic: {}", cachedTopic.getTopic().getQualifiedName());
            }
        }
    }

    private boolean isBrokerTopicAvailable(CachedTopic cachedTopic) {
        if (topicsAvailabilityCache.contains(cachedTopic.getTopic())) {
            return true;
        }

        if (brokerMessageProducer.isTopicAvailable(cachedTopic)) {
            topicsAvailabilityCache.add(cachedTopic.getTopic());
            logger.info("Broker topic {} is available.", cachedTopic.getTopic().getQualifiedName());
            return true;
        }

        return false;
    }

    private boolean isNotStale(Message message) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(message.getTimestamp()), ZoneId.systemDefault())
                .isAfter(LocalDateTime.now().minusHours(messageMaxAgeHours));
    }

    private void sendMessage(Message message, CachedTopic cachedTopic) {
        StartedTimersPair brokerTimers = cachedTopic.startBrokerLatencyTimers();
        brokerMessageProducer.send(message, cachedTopic, new PublishingCallback() {
            @Override
            public void onUnpublished(Message message, Topic topic, Exception exception) {
                brokerTimers.close();
                brokerListeners.onError(message, topic, exception);
                trackers.get(topic).logError(message.getId(), topic.getName(), exception.getMessage(), "");
                toResend.get().add(ImmutablePair.of(message, cachedTopic));
            }

            @Override
            public void onPublished(Message message, Topic topic) {
                brokerTimers.close();
                cachedTopic.incrementPublished();
                brokerListeners.onAcknowledge(message, topic);
                trackers.get(topic).logPublished(message.getId(), topic.getName(), "");
            }
        });
    }
}
