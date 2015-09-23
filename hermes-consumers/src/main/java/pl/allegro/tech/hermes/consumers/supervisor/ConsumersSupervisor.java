package pl.allegro.tech.hermes.consumers.supervisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.admin.AdminOperationsCallback;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsStorage;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetCommitter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffsets;
import pl.allegro.tech.hermes.domain.subscription.offset.SubscriptionOffsetChangeIndicator;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static pl.allegro.tech.hermes.api.Subscription.State.ACTIVE;
import static pl.allegro.tech.hermes.api.Subscription.State.PENDING;
import static pl.allegro.tech.hermes.api.Subscription.State.SUSPENDED;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;

public class ConsumersSupervisor implements AdminOperationsCallback {


    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumersSupervisor.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator;

    private final ConsumersExecutorService executor;
    private final ConsumerFactory consumerFactory;
    private final List<OffsetsStorage> offsetsStorages;
    private final List<MessageCommitter> messageCommitters;
    private final HermesMetrics hermesMetrics;
    private final OffsetCommitter offsetCommitter;
    private final ConsumerHolder consumerHolder;
    private final ZookeeperAdminCache adminCache;
    private final SubscriptionLocks subscriptionsLocks;

    private final String brokersClusterName;
    private final UndeliveredMessageLogPersister undeliveredMessageLogPersister;

    @Inject
    public ConsumersSupervisor(ConfigFactory configFactory,
                               SubscriptionRepository subscriptionRepository,
                               SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator,
                               ConsumersExecutorService executor,
                               ConsumerFactory consumerFactory,
                               List<MessageCommitter> messageCommitters,
                               List<OffsetsStorage> offsetsStorages,
                               HermesMetrics hermesMetrics,
                               ZookeeperAdminCache adminCache,
                               UndeliveredMessageLogPersister undeliveredMessageLogPersister) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionOffsetChangeIndicator = subscriptionOffsetChangeIndicator;
        this.executor = executor;
        this.consumerFactory = consumerFactory;
        this.offsetsStorages = offsetsStorages;
        this.messageCommitters = messageCommitters;
        this.adminCache = adminCache;
        this.hermesMetrics = hermesMetrics;
        this.undeliveredMessageLogPersister = undeliveredMessageLogPersister;

        this.subscriptionsLocks = new SubscriptionLocks();

        consumerHolder = new ConsumerHolder();
        offsetCommitter = new OffsetCommitter(consumerHolder, messageCommitters, configFactory);

        brokersClusterName = configFactory.getStringProperty(KAFKA_CLUSTER_NAME);
    }

    public void assignConsumerForSubscription(Subscription subscription) {
        synchronized (subscriptionsLocks.getLock(subscription)) {
            try {
                if (subscription.getState() == PENDING) {
                    createAndExecuteConsumerIfNotExists(subscription);
                    activateSubscription(subscription);
                } else if (subscription.getState() == ACTIVE) {
                    createAndExecuteConsumerIfNotExists(subscription);
                } else {
                    LOGGER.info("Got subscription created event for inactive subscription {}", subscription.getId());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to create subscription " + subscription.getName(), e);
            }
        }
    }

    public void deleteConsumerForSubscription(Subscription subscription) {
        synchronized (subscriptionsLocks.getLock(subscription)) {
            try {
                deleteConsumerIfExists(subscription, true);
                hermesMetrics.removeMetrics(subscription);
            } catch (Exception e) {
                LOGGER.error("Failed to remove subscription " + subscription.getId(), e);
            }
        }
    }

    @Deprecated
    public void notifyConsumerOnSubscriptionUpdate(Subscription modifiedSubscription) {
        synchronized (subscriptionsLocks.getLock(modifiedSubscription)) {
            try {
                Optional<Consumer> consumerOptional = consumerHolder.get(modifiedSubscription.getTopicName(), modifiedSubscription.getName());

                Subscription.State oldState = consumerOptional.map((consumer) -> consumer.getSubscription().getState()).orElse(SUSPENDED);
                if (subscriptionStateChanged(modifiedSubscription, oldState)) {
                    handleSubscriptionStateChange(oldState, modifiedSubscription.getState(), modifiedSubscription);
                }

                consumerOptional.ifPresent((consumer) -> consumer.updateSubscription(modifiedSubscription));

            } catch (Exception e) {
                LOGGER.error("Failed to update subscription " + modifiedSubscription.getId(), e);
            }
        }
    }

    public void updateSubscription(Subscription modifiedSubscription) {
        synchronized (subscriptionsLocks.getLock(modifiedSubscription)) {
            consumerHolder.get(modifiedSubscription.getTopicName(), modifiedSubscription.getName()).
                    ifPresent((consumer) -> consumer.updateSubscription(modifiedSubscription));
        }
    }

    private void activateSubscription(Subscription subscription) {
        subscription.setState(Subscription.State.ACTIVE);
        subscriptionRepository.updateSubscription(subscription);
    }

    @Deprecated
    private boolean subscriptionStateChanged(Subscription modifiedSubscription, Subscription.State oldState) {
        return oldState != modifiedSubscription.getState() && oldState != PENDING;
    }

    @Deprecated
    private void handleSubscriptionStateChange(Subscription.State oldState,
                                               Subscription.State newState,
                                               Subscription modifiedSubscription) throws Exception {
        LOGGER.info("Changing state from {} to {} for subscription {}", oldState, newState, modifiedSubscription.getId());
        switch (newState) {
            case PENDING:
                if (!oldState.equals(PENDING)) {
                    createAndExecuteConsumerIfNotExists(modifiedSubscription);
                    activateSubscription(modifiedSubscription);
                }
                break;
            case ACTIVE:
                if (!oldState.equals(ACTIVE)) {
                    createAndExecuteConsumerIfNotExists(modifiedSubscription);
                }
                break;
            case SUSPENDED:
                if (oldState.equals(ACTIVE)) {
                    deleteConsumerIfExists(modifiedSubscription, false);
                }
                break;
            default:
                break;
        }
    }

    public void start() throws Exception {
        adminCache.start();
        adminCache.addCallback(this);
        offsetCommitter.start();
        undeliveredMessageLogPersister.start();
    }

    public void shutdown() throws InterruptedException {
        for (Consumer consumer : consumerHolder) {
            consumer.stopConsuming();
        }
        executor.shutdown();
        offsetCommitter.shutdown();
        undeliveredMessageLogPersister.shutdown();
    }

    private void createAndExecuteConsumerIfNotExists(Subscription subscription) {
            if (consumerHolder.contains(subscription.getTopicName(), subscription.getName())) {
                LOGGER.warn("Consumer for {} already exists, ignoring", subscription.getId());
            } else {
                createAndExecuteConsumer(subscription);
            }
    }

    private void deleteConsumerIfExists(Subscription subscription, boolean removeOffsets) throws Exception {
        deleteConsumerIfExists(subscription.getTopicName(), subscription.getName(), removeOffsets);
    }

    private void deleteConsumerIfExists(SubscriptionName subscription, boolean removeOffsets) throws Exception {
        deleteConsumerIfExists(subscription.getTopicName(), subscription.getName(), removeOffsets);
    }

    private void deleteConsumerIfExists(TopicName topicName, String subscriptionName, boolean removeOffsets) throws Exception {
        if (consumerHolder.contains(topicName, subscriptionName)) {
            LOGGER.info("Deleting consumer for {}", Subscription.getId(topicName, subscriptionName));
            Consumer consumer = consumerHolder.get(topicName, subscriptionName).get();
            consumer.stopConsuming();
            consumerHolder.remove(topicName, subscriptionName);
            if (removeOffsets) {
                removeOffsets(topicName, subscriptionName, consumer.getOffsetsToCommit());
            }
        }
    }

    private void createAndExecuteConsumer(Subscription subscription) {
        LOGGER.info("Creating consumer for {}", subscription.getId());
        Consumer consumer = consumerFactory.createConsumer(subscription);
        consumerHolder.add(subscription.getTopicName(), subscription.getName(), consumer);
        executor.execute(consumer);
    }

    private void removeOffsets(TopicName topicName, String subscriptionName, List<PartitionOffset> offsetsToRemove) throws Exception {
        for (PartitionOffset partitionOffset : offsetsToRemove) {
            for (MessageCommitter messageCommitter: messageCommitters) {
                messageCommitter.removeOffset(topicName, subscriptionName, partitionOffset.getPartition());
            }
        }
    }

    @Override
    public void onRetransmissionStarts(SubscriptionName subscriptionName) throws Exception {
        synchronized (subscriptionsLocks.getLock(subscriptionName)) {
            deleteConsumerIfExists(subscriptionName, false);

            PartitionOffsets offsets = subscriptionOffsetChangeIndicator.getSubscriptionOffsets(
                    subscriptionName.getTopicName(), subscriptionName.getName(), brokersClusterName);

            for (PartitionOffset partitionOffset : offsets) {
                for (OffsetsStorage s: offsetsStorages) {
                    s.setSubscriptionOffset(Subscription.fromSubscriptionName(subscriptionName), partitionOffset);
                }
            }
            createAndExecuteConsumer(subscriptionRepository.getSubscriptionDetails(subscriptionName.getTopicName(), subscriptionName.getName()));
        }
    }
}
