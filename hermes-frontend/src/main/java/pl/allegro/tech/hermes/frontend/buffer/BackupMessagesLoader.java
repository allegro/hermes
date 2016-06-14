package pl.allegro.tech.hermes.frontend.buffer;

import com.google.common.collect.Lists;
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
import pl.allegro.tech.hermes.frontend.publishing.callbacks.MetricsPublishingCallback;
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
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;
import static pl.allegro.tech.hermes.common.config.Configs.*;

public class BackupMessagesLoader {

    private static final Logger logger = LoggerFactory.getLogger(BackupMessagesLoader.class);
    private static final int THREAD_POOL_SIZE = 16;

    private final BrokerMessageProducer brokerMessageProducer;
    private final HermesMetrics hermesMetrics;
    private final BrokerListeners brokerListeners;
    private final TopicsCache topicsCache;
    private final Trackers trackers;
    private final int secondsToWaitForTopicsCache;
    private final int messageMaxAgeHours;
    private final int maxResendRetries;
    private final long resendSleep;
    private final long readTopicInfoSleep;

    private final Set<Topic> topicsAvailabilityCache = new HashSet<>();
    private final AtomicReference<ConcurrentLinkedQueue<Pair<Message, Topic>>> toResend = new AtomicReference<>();

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
        this.resendSleep = config.getIntProperty(KAFKA_PRODUCER_ACK_TIMEOUT) + secondsToWaitForTopicsCache * 1000;
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
                List<Pair<Message, Topic>> retryMessages = Lists.newArrayList(toResend.getAndSet(new ConcurrentLinkedQueue<>()));
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
        ExecutorService executor = createExecutor();
        try {
            int sentCounter = 0;
            int discardedCounter = 0;
            for (BackupMessage backupMessage : messages) {
                Message message = new JsonMessage(backupMessage.getMessageId(), backupMessage.getData(), backupMessage.getTimestamp());
                Optional<Topic> topic = loadTopic(backupMessage.getQualifiedTopicName(), executor);
                if (sendMessageIfNeeded(message, topic, "sending")) {
                    sentCounter++;
                } else {
                    discardedCounter++;
                }
            }
            logger.info("Loaded and sent {} messages and discarded {} messages from the backup storage.", sentCounter, discardedCounter);
        } finally {
            shutdownExecutor(executor);
        }
    }

    private ExecutorService createExecutor() {
        return Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    private void shutdownExecutor(ExecutorService executor) {
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                logger.error("Termination of load topics executor service interrupted.", e);
            }
        }
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

        logger.info("Resent {}/{} messages and discarded {} messages from the backup storage retry {}.", sentCounter, messageAndTopicList.size(), discardedCounter, retry);
    }

    private boolean sendMessageIfNeeded(Message message, Optional<Topic> topic, String contextName) {
        if (topic.isPresent() && isNotStale(message)) {
            waitOnBrokerTopicAvailability(topic.get());
            sendMessage(message, topic.get());
            return true;
        } else {
            String topicName = topic.map(t -> t.getName().qualifiedName()).orElse("missing-topic-info");
            logger.warn("Not {} stale message {} {} {}", contextName, message.getId(), topicName,
                    new String(message.getData(), Charset.defaultCharset()));
            return false;
        }
    }

    private void waitOnBrokerTopicAvailability(Topic topic) {
        int tries = 0;
        while(!isBrokerTopicAvailable(topic)) {
            try {
                tries++;
                logger.info("Broker topic {} is not available, checked {} times.", topic.getQualifiedName(), tries);
                Thread.sleep(readTopicInfoSleep);
            } catch (InterruptedException e) {
                logger.warn("Waiting for broker topic availability interrupted. Topic: {}", topic.getQualifiedName());
            }
        }
    }

    private boolean isBrokerTopicAvailable(Topic topic) {
        if (topicsAvailabilityCache.contains(topic)) {
            return true;
        }

        if (brokerMessageProducer.isTopicAvailable(topic)) {
            topicsAvailabilityCache.add(topic);
            logger.info("Broker topic {} is available.", topic.getQualifiedName());
            return true;
        }

        return false;
    }

    private boolean isNotStale(Message message) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(message.getTimestamp()), ZoneId.systemDefault())
                .isAfter(LocalDateTime.now().minusHours(messageMaxAgeHours));
    }

    private void sendMessage(Message message, Topic topic) {
        brokerMessageProducer.send(message, topic, new SimpleExecutionCallback(
                new MetricsPublishingCallback(hermesMetrics, topic),
                new PublishingCallback() {
                    @Override
                    public void onUnpublished(Message message, Topic topic, Exception exception) {
                        brokerListeners.onError(message, topic, exception);
                        trackers.get(topic).logError(message.getId(), topic.getName(), exception.getMessage());
                        toResend.get().add(ImmutablePair.of(message, topic));
                    }

                    @Override
                    public void onPublished(Message message, Topic topic) {
                        brokerListeners.onAcknowledge(message, topic);
                        trackers.get(topic).logPublished(message.getId(), topic.getName());
                    }
                }));
    }

    private Optional<Topic> loadTopic(String topicName, Executor executor) {
        try {
            return CompletableFuture.supplyAsync(() -> topicsCache.getTopic(topicName), executor)
                    .get(secondsToWaitForTopicsCache, SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Could not read topic {} from topics cache after {} seconds", topicName, secondsToWaitForTopicsCache);
            return Optional.empty();
        }
    }

}
