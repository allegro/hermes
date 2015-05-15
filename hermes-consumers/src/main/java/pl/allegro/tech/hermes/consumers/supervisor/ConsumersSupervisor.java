package pl.allegro.tech.hermes.consumers.supervisor;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.admin.AdminOperationsCallback;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.broker.BrokerStorage;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.offset.AsyncOffsetMonitor;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetCommitter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionCallback;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffsets;
import pl.allegro.tech.hermes.domain.subscription.offset.SubscriptionOffsetChangeIndicator;

import javax.inject.Inject;
import java.util.List;

import static pl.allegro.tech.hermes.api.Subscription.State.ACTIVE;
import static pl.allegro.tech.hermes.api.Subscription.State.PENDING;
import static pl.allegro.tech.hermes.api.Subscription.State.SUSPENDED;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;

public class ConsumersSupervisor implements SubscriptionCallback, AdminOperationsCallback {


    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumersSupervisor.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator;

    private final ConsumersExecutorService executor;
    private final ConsumerFactory consumerFactory;
    private final BrokerStorage brokerStorage;
    private final MessageCommitter messageCommitter;
    private final HermesMetrics hermesMetrics;
    private final OffsetCommitter offsetCommitter;
    private final ConsumerHolder consumerHolder;
    private final SubscriptionsCache subscriptionsCache;
    private final ZookeeperAdminCache adminCache;
    private final SubscriptionLocks subscriptionsLocks;

    private final String brokersClusterName;

    @Inject
    public ConsumersSupervisor(ConfigFactory configFactory,
                               SubscriptionRepository subscriptionRepository,
                               SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator,
                               ConsumersExecutorService executor,
                               ConsumerFactory consumerFactory,
                               MessageCommitter messageCommitter,
                               BrokerStorage brokerStorage,
                               SubscriptionsCache subscriptionsCache,
                               HermesMetrics hermesMetrics,
                               AsyncOffsetMonitor asyncOffsetMonitor,
                               ZookeeperAdminCache adminCache) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionOffsetChangeIndicator = subscriptionOffsetChangeIndicator;
        this.executor = executor;
        this.consumerFactory = consumerFactory;
        this.brokerStorage = brokerStorage;
        this.messageCommitter = messageCommitter;
        this.subscriptionsCache = subscriptionsCache;
        this.adminCache = adminCache;
        this.hermesMetrics = hermesMetrics;

        this.subscriptionsLocks = new SubscriptionLocks();

        consumerHolder = new ConsumerHolder();
        offsetCommitter = new OffsetCommitter(consumerHolder, messageCommitter, configFactory, asyncOffsetMonitor);
        offsetCommitter.start();

        brokersClusterName = configFactory.getStringProperty(KAFKA_CLUSTER_NAME);
    }

    @Override
    public void onSubscriptionCreated(Subscription subscription) {
        try {
            if (subscription.getState() == PENDING) {
                createAndExecuteConsumerIfNotExists(subscription);
                activateSubscription(subscription);
            } else if (subscription.getState() == ACTIVE) {
                createAndExecuteConsumerIfNotExists(subscription);
            } else {
                LOGGER.info("Got subscription created event for inactive subscription {}", subscription.getName());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create subscription " + subscription.getName(), e);
        }
    }

    @Override
    public void onSubscriptionRemoved(Subscription subscription) {
        try {
            deleteConsumer(subscription, true);
            hermesMetrics.removeMetrics(subscription);
        } catch (Exception e) {
            LOGGER.error("Failed to remove subscription " + subscription.getName(), e);
        }
    }

    @Override
    public void onSubscriptionChanged(Subscription modifiedSubscription) {
        try {
            Optional<Consumer> consumer = consumerHolder.get(modifiedSubscription.getTopicName(), modifiedSubscription.getName());

            Subscription.State oldState = consumer.isPresent() ? consumer.get().getSubscription().getState() : SUSPENDED;
            if (subscriptionStateChanged(modifiedSubscription, oldState)) {
                handleSubscriptionStateChange(oldState, modifiedSubscription.getState(), modifiedSubscription);
            }

            if (consumer.isPresent()) {
                consumer.get().updateSubscription(modifiedSubscription);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update subscription " + modifiedSubscription.getName(), e);
        }
    }

    private void activateSubscription(Subscription subscription) {
        subscription.setState(Subscription.State.ACTIVE);
        subscriptionRepository.updateSubscription(subscription);
    }

    private boolean subscriptionStateChanged(Subscription modifiedSubscription, Subscription.State oldState) {
        return oldState != modifiedSubscription.getState() && oldState != PENDING;
    }

    private void handleSubscriptionStateChange(Subscription.State oldState,
                                               Subscription.State newState,
                                               Subscription modifiedSubscription) throws Exception {
        LOGGER.info("Changing subscription state from {} to {}", oldState, newState);
        switch (newState) {
            case PENDING:
                if (!oldState.equals(PENDING)) {
                    createAndExecuteConsumerIfNotExists(modifiedSubscription);
                    activateSubscription(modifiedSubscription);
                }
            case ACTIVE:
                if (!oldState.equals(ACTIVE)) {
                    createAndExecuteConsumerIfNotExists(modifiedSubscription);
                }
                break;
            case SUSPENDED:
                if (oldState.equals(ACTIVE)) {
                    deleteConsumer(modifiedSubscription, false);
                }
                break;
            default:
                break;
        }
    }

    public void start() throws Exception {
        subscriptionsCache.start(ImmutableList.of(this));
        adminCache.start();
        adminCache.addCallback(this);
    }

    public void shutdown() throws InterruptedException {
        for (Consumer consumer : consumerHolder) {
            consumer.stopConsuming();
        }
        executor.shutdown();

        offsetCommitter.shutdown();
    }

    private void createAndExecuteConsumerIfNotExists(Subscription subscription) {
        synchronized (subscriptionsLocks.getLock(subscription.getId())) {
            Optional<Consumer> maybeConsumer = consumerHolder.get(subscription.getTopicName(), subscription.getName());
            if (maybeConsumer.isPresent()) {
                LOGGER.warn("Consumer for {} already exists, ignoring", subscription.getId());
            } else {
                createAndExecuteConsumer(subscription);
            }
        }
    }

    private void deleteConsumer(Subscription subscription, boolean removeOffsets) throws Exception {
        deleteConsumer(subscription.getTopicName(), subscription.getName(), removeOffsets);
    }

    private void deleteConsumer(SubscriptionName subscription, boolean removeOffsets) throws Exception {
        deleteConsumer(subscription.getTopicName(), subscription.getName(), removeOffsets);
    }

    private void deleteConsumer(TopicName topicName, String subscriptionName, boolean removeOffsets) throws Exception {
        synchronized (subscriptionsLocks.getLock(Subscription.getId(topicName, subscriptionName))) {
            Optional<Consumer> maybeConsumer = consumerHolder.get(topicName, subscriptionName);
            if (maybeConsumer.isPresent()) {
                Consumer consumer = maybeConsumer.get();
                consumer.stopConsuming();
                consumerHolder.remove(topicName, subscriptionName);
                if (removeOffsets) {
                    removeOffsets(topicName, subscriptionName, consumer.getOffsetsToCommit());
                }
            }
        }
    }

    private void createAndExecuteConsumer(Subscription subscription) {
        Consumer consumer = consumerFactory.createConsumer(subscription);
        consumerHolder.add(subscription.getTopicName(), subscription.getName(), consumer);
        executor.execute(consumer);
    }

    private void removeOffsets(TopicName topicName, String subscriptionName, List<PartitionOffset> offsetsToRemove) throws Exception {
        for (PartitionOffset partitionOffset : offsetsToRemove) {
            messageCommitter.removeOffset(topicName, subscriptionName, partitionOffset.getPartition());
        }
    }

    @Override
    public void onRetransmissionStarts(SubscriptionName subscription) throws Exception {
        deleteConsumer(subscription, false);

        PartitionOffsets offsets = subscriptionOffsetChangeIndicator.getSubscriptionOffsets(
            subscription.getTopicName(), subscription.getName(), brokersClusterName);

        for (PartitionOffset partitionOffset : offsets) {
            brokerStorage.setSubscriptionOffset(subscription.getTopicName(), subscription.getName(), partitionOffset.getPartition(),
                    partitionOffset.getOffset());
        }
        createAndExecuteConsumer(subscriptionRepository.getSubscriptionDetails(subscription.getTopicName(), subscription.getName()));
    }
}
