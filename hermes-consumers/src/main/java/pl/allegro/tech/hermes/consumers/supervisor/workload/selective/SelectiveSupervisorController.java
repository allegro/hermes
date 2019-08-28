package pl.allegro.tech.hermes.consumers.supervisor.workload.selective;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorController;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkTracker;
import pl.allegro.tech.hermes.consumers.supervisor.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_ALGORITHM;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_AUTO_REBALANCE;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_CONSUMERS_PER_SUBSCRIPTION;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_MAX_SUBSCRIPTIONS_PER_CONSUMER;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_NODE_ID;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_REBALANCE_INTERVAL;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;

public class SelectiveSupervisorController implements SupervisorController {

    private static final Logger logger = LoggerFactory.getLogger(SelectiveSupervisorController.class);

    private final ConsumersSupervisor supervisor;
    private final InternalNotificationsBus notificationsBus;
    private final SubscriptionsCache subscriptionsCache;
    private final SubscriptionAssignmentRegistry registry;
    private final WorkTracker workTracker;
    private final ConsumerNodesRegistry consumersRegistry;
    private final BalancingJob balancingJob;
    private final ZookeeperAdminCache adminCache;
    private final ConfigFactory configFactory;

    private final ExecutorService assignmentExecutor;

    public SelectiveSupervisorController(ConsumersSupervisor supervisor,
                                         InternalNotificationsBus notificationsBus,
                                         SubscriptionsCache subscriptionsCache,
                                         SubscriptionAssignmentRegistry registry,
                                         WorkTracker workTracker,
                                         ConsumerNodesRegistry consumersRegistry,
                                         ZookeeperAdminCache adminCache,
                                         ExecutorService assignmentExecutor,
                                         ConfigFactory configFactory,
                                         HermesMetrics metrics,
                                         WorkloadConstraintsRepository workloadConstraintsRepository) {

        this.supervisor = supervisor;
        this.notificationsBus = notificationsBus;
        this.subscriptionsCache = subscriptionsCache;
        this.registry = registry;
        this.workTracker = workTracker;
        this.consumersRegistry = consumersRegistry;
        this.adminCache = adminCache;
        this.assignmentExecutor = assignmentExecutor;
        this.configFactory = configFactory;
        this.balancingJob = new BalancingJob(
                consumersRegistry,
                configFactory,
                subscriptionsCache,
                new SelectiveWorkBalancer(),
                workTracker, metrics,
                configFactory.getIntProperty(CONSUMER_WORKLOAD_REBALANCE_INTERVAL),
                configFactory.getStringProperty(KAFKA_CLUSTER_NAME),
                workloadConstraintsRepository);
    }

    @Override
    public void onSubscriptionAssigned(SubscriptionName subscriptionName) {
        Subscription subscription = subscriptionsCache.getSubscription(subscriptionName);
        logger.info("Scheduling assignment consumer for {}", subscription.getQualifiedName());
        assignmentExecutor.execute(() -> {
            logger.info("Assigning consumer for {}", subscription.getQualifiedName());
            supervisor.assignConsumerForSubscription(subscription);
            logger.info("Consumer assigned for {}", subscription.getQualifiedName());
        });
    }

    @Override
    public void onAssignmentRemoved(SubscriptionName subscription) {
        logger.info("Scheduling assignment removal consumer for {}", subscription.getQualifiedName());
        assignmentExecutor.execute(() -> {
            logger.info("Removing assignment from consumer for {}", subscription.getQualifiedName());
            supervisor.deleteConsumerForSubscriptionName(subscription);
            logger.info("Consumer removed for {}", subscription.getName());
        });
    }

    @Override
    public void onSubscriptionChanged(Subscription subscription) {
        if (workTracker.isAssignedTo(subscription.getQualifiedName(), consumerId())) {
            logger.info("Updating subscription {}", subscription.getName());
            supervisor.updateSubscription(subscription);
        }
    }

    @Override
    public void onTopicChanged(Topic topic) {
        for (Subscription subscription : subscriptionsCache.subscriptionsOfTopic(topic.getName())) {
            if(workTracker.isAssignedTo(subscription.getQualifiedName(), consumerId())) {
                supervisor.updateTopic(subscription, topic);
            }
        }
    }

    @Override
    public void start() throws Exception {
        long startTime = System.currentTimeMillis();

        adminCache.start();
        adminCache.addCallback(this);

        notificationsBus.registerSubscriptionCallback(this);
        notificationsBus.registerTopicCallback(this);
        registry.registerAssignmentCallback(this);

        supervisor.start();
        consumersRegistry.start();
        registry.start();
        if (configFactory.getBooleanProperty(CONSUMER_WORKLOAD_AUTO_REBALANCE)) {
            balancingJob.start();
            consumersRegistry.startLeaderLatch();
        } else {
            logger.info("Automatic workload rebalancing is disabled.");
        }

        logger.info("Consumer boot complete in {} ms. Workload config: [{}]",
                System.currentTimeMillis() - startTime,
                configFactory.print(
                        CONSUMER_WORKLOAD_NODE_ID,
                        CONSUMER_WORKLOAD_ALGORITHM,
                        CONSUMER_WORKLOAD_REBALANCE_INTERVAL,
                        CONSUMER_WORKLOAD_CONSUMERS_PER_SUBSCRIPTION,
                        CONSUMER_WORKLOAD_MAX_SUBSCRIPTIONS_PER_CONSUMER));
    }

    @Override
    public Set<SubscriptionName> assignedSubscriptions() {
        return registry.createSnapshot().getSubscriptionsForConsumerNode(consumerId());
    }

    @Override
    public void shutdown() throws InterruptedException {
        balancingJob.stop();
        consumersRegistry.stopLeaderLatch();
        supervisor.shutdown();
    }

    @Override
    public Optional<String> watchedConsumerId() {
        return Optional.of(consumerId());
    }

    public String consumerId() {
        return consumersRegistry.getId();
    }

    public boolean isLeader() {
        return consumersRegistry.isLeader();
    }

    @Override
    public void onRetransmissionStarts(SubscriptionName subscription) throws Exception {
        logger.info("Triggering retransmission for subscription {}", subscription);
        if (workTracker.isAssignedTo(subscription, consumerId())) {
            supervisor.retransmit(subscription);
        }
    }
}
