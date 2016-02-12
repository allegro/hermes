package pl.allegro.tech.hermes.frontend.buffer;

import com.jayway.awaitility.core.ConditionTimeoutException;
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
import pl.allegro.tech.hermes.frontend.publishing.callbacks.BrokerListenersPublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.callbacks.MetricsPublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOADING_WAIT_FOR_TOPICS_CACHE;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_MAX_AGE_HOURS;

public class BackupMessagesLoader {

    private static final Logger logger = LoggerFactory.getLogger(BackupMessagesLoader.class);

    private final BrokerMessageProducer brokerMessageProducer;
    private final HermesMetrics hermesMetrics;
    private final BrokerListeners brokerListeners;
    private final TopicsCache topicsCache;
    private final Trackers trackers;
    private final int secondsToWaitForTopicsCache;
    private final int messageMaxAgeHours;

    @Inject
    public BackupMessagesLoader(BrokerMessageProducer brokerMessageProducer,
                                HermesMetrics hermesMetrics,
                                BrokerListeners brokerListeners,
                                TopicsCache topicsCache,
                                Trackers trackers,
                                ConfigFactory config) {
        this.brokerMessageProducer = brokerMessageProducer;
        this.hermesMetrics = hermesMetrics;
        this.brokerListeners = brokerListeners;
        this.topicsCache = topicsCache;
        this.trackers = trackers;
        this.secondsToWaitForTopicsCache = config.getIntProperty(MESSAGES_LOADING_WAIT_FOR_TOPICS_CACHE);
        this.messageMaxAgeHours = config.getIntProperty(MESSAGES_LOCAL_STORAGE_MAX_AGE_HOURS);
    }

    public void loadMessages(MessageRepository messageRepository) {
        List<BackupMessage> messages = messageRepository.findAll();

        logger.info("Loading {} messages from backup storage.", messages.size());

        int sentCounter = 0;
        int discardedCounter = 0;
        for (BackupMessage backupMessage : messages) {
            Message message = new Message(backupMessage.getMessageId(), backupMessage.getData(), backupMessage.getTimestamp());
            Optional<Topic> topic = loadTopic(fromQualifiedName(backupMessage.getQualifiedTopicName()));
            if (topic.isPresent() && isNotStale(backupMessage)) {
                sentCounter++;
                sendMessage(message, topic.get());
            } else {
                discardedCounter++;
                logger.warn("Not sending stale message {} {} {}", backupMessage.getMessageId(), backupMessage.getQualifiedTopicName(),
                        new String(backupMessage.getData(), Charset.defaultCharset()));
            }
        }

        logger.info("Loaded and sent {} messages and discarded {} messages from the backup storage", sentCounter, discardedCounter);
    }

    private boolean isNotStale(BackupMessage backupMessage) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(backupMessage.getTimestamp()), ZoneId.systemDefault())
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
