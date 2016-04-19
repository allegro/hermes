package pl.allegro.tech.hermes.frontend.buffer;

import com.jayway.awaitility.core.ConditionTimeoutException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.avro.AvroMessage;
import pl.allegro.tech.hermes.frontend.publishing.callbacks.BrokerListenersPublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.callbacks.MetricsPublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageFactory;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;
import static pl.allegro.tech.hermes.common.config.Configs.*;

public class BackupMessagesLoader {

    private static final Logger logger = LoggerFactory.getLogger(BackupMessagesLoader.class);

    private final BrokerMessageProducer brokerMessageProducer;
    private final HermesMetrics hermesMetrics;
    private final BrokerListeners brokerListeners;
    private final TopicsCache topicsCache;
    private final Trackers trackers;
    private final int secondsToWaitForTopicsCache;
    private final int messageMaxAgeHours;
    private final int resendSleep;
    private final int maxResendRetries;
    private final AtomicReference<ConcurrentLinkedQueue<Pair<Message, Topic>>> toResend = new AtomicReference<>();
    private final MessageFactory messageFactory;

    @Inject
    public BackupMessagesLoader(BrokerMessageProducer brokerMessageProducer,
                                HermesMetrics hermesMetrics,
                                BrokerListeners brokerListeners,
                                TopicsCache topicsCache,
                                Trackers trackers,
                                ConfigFactory config,
                                MessageFactory messageFactory) {
        this.brokerMessageProducer = brokerMessageProducer;
        this.hermesMetrics = hermesMetrics;
        this.brokerListeners = brokerListeners;
        this.topicsCache = topicsCache;
        this.trackers = trackers;
        this.messageFactory = messageFactory;
        this.secondsToWaitForTopicsCache = config.getIntProperty(MESSAGES_LOADING_WAIT_FOR_TOPICS_CACHE);
        this.messageMaxAgeHours = config.getIntProperty(MESSAGES_LOCAL_STORAGE_MAX_AGE_HOURS);
        this.resendSleep = config.getIntProperty(KAFKA_PRODUCER_ACK_TIMEOUT) + secondsToWaitForTopicsCache * 1000;
        this.maxResendRetries = config.getIntProperty(MESSAGES_LOCAL_STORAGE_MAX_RESEND_RETRIES);
    }

    public void loadMessages(MessageRepository messageRepository) {
        List<BackupMessage> messages = messageRepository.findAll();
        logger.info("Loading {} messages from backup storage.", messages.size());
        int retry = 0;
        toResend.set(new ConcurrentLinkedQueue<>());

        sendMessages(messages);

        do {
            if (retry > 0) {
                List<Pair<Message, Topic>> retryMessages = new ArrayList(toResend.getAndSet(new ConcurrentLinkedQueue<>()));
                resendMessages(retryMessages, retry);
            }
            try {
                Thread.sleep(resendSleep);
            } catch (InterruptedException e) {
                logger.warn("Sleep interrupted", e);
            }
            retry++;
        } while (toResend.get().size() > 0 && retry <= maxResendRetries);

        logger.info("Finished resending messages from backup storage after retry #{} with #{} not sent messages.", retry, toResend.get().size());
    }

    private void sendMessages(List<BackupMessage> messages) {
        logger.info("Sending {} messages from backup storage.", messages.size());

        int sentCounter = 0;
        int discardedCounter = 0;
        for (BackupMessage backupMessage : messages) {
            TopicName topicName = fromQualifiedName(backupMessage.getQualifiedTopicName());
            Optional<Topic> topic = loadTopic(topicName);

            if (topic.isPresent()) {
                Message message = messageFactory.create(topic.get(), backupMessage.getMessageId(), backupMessage.getData(), backupMessage.getTimestamp(), backupMessage.getSchemaVersionOptional());
                if (sendMessageIfNeeded(message, topic, "sending")) {
                    sentCounter++;
                } else {
                    discardedCounter++;
                }
            } else {
                logger.info("Discarded message, no topic for name {}.", topicName );
                discardedCounter++;
            }
        }

        logger.info("Loaded and sent {} messages and discarded {} messages from the backup storage.", sentCounter, discardedCounter);
    }

    private void resendMessages(List<Pair<Message, Topic>> messageAndTopicList, int retry) {
        logger.info("Resending {} messages from backup storage retry {}.", messageAndTopicList.size(), retry);

        int sentCounter = 0;
        int discardedCounter = 0;
        for (Pair<Message, Topic> messageAndTopic : messageAndTopicList) {
            Message message = messageAndTopic.getKey();
            Optional<Topic> topic = Optional.of(messageAndTopic.getValue());
            if (sendMessageIfNeeded(message, topic, "resending")) {
                sentCounter++;
            } else {
                discardedCounter++;
            }
        }

        logger.info("Resent {} messages and discarded {} messages from the backup storage retry {}.", sentCounter, discardedCounter, retry);
    }

    private boolean sendMessageIfNeeded(Message message, Optional<Topic> topic, String contextName) {
        if (topic.isPresent() && isNotStale(message)) {
            sendMessage(message, topic.get());
            return true;
        } else {
            logger.warn("Not {} stale message {} {} {}", contextName, message.getId(), topic.get().getQualifiedName(),
                    new String(message.getData(), Charset.defaultCharset()));
            return false;
        }
    }

    private boolean isNotStale(Message message) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(message.getTimestamp()), ZoneId.systemDefault())
                .isAfter(LocalDateTime.now().minusHours(messageMaxAgeHours));
    }

    private void sendMessage(Message message, Topic topic) {
        brokerMessageProducer.send(message, topic, new SimpleExecutionCallback(
                new MetricsPublishingCallback(hermesMetrics, topic),
                new BrokerListenersPublishingCallback(brokerListeners),
                new PublishingCallback() {
                    @Override
                    public void onUnpublished(Message message, Topic topic, Exception exception) {
                        trackers.get(topic).logError(message.getId(), topic.getName(), exception.getMessage());
                        toResend.get().add(ImmutablePair.of(message, topic));
                    }

                    @Override
                    public void onPublished(Message message, Topic topic) {
                        trackers.get(topic).logPublished(message.getId(), topic.getName());
                    }
                }));
    }

    private Optional<Topic> loadTopic(TopicName topicName) {
        try {
            await().pollDelay(1, TimeUnit.NANOSECONDS).atMost(secondsToWaitForTopicsCache, SECONDS).until(() -> topicsCache.getTopic(topicName).isPresent());
            return topicsCache.getTopic(topicName);
        } catch (ConditionTimeoutException timeout) {
            logger.error("Could not read topic {} from topics cache after {} seconds", topicName, secondsToWaitForTopicsCache);
            return Optional.empty();
        }
    }

}
