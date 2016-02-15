package pl.allegro.tech.hermes.consumers.supervisor.workload.selective;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorController;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkTracker;

import java.util.Collections;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_ALGORITHM;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_CONSUMERS_PER_SUBSCRIPTION;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_NODE_ID;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_MAX_SUBSCRIPTIONS_PER_CONSUMER;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_REBALANCE_INTERVAL;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;

public class SelectiveSupervisorController implements SupervisorController {
    private ConsumersSupervisor supervisor;
    private SubscriptionsCache subscriptionsCache;
    private WorkTracker workTracker;
    private ConsumerNodesRegistry consumersRegistry;
    private final ZookeeperAdminCache adminCache;
    private ConfigFactory configFactory;
    private HermesMetrics metrics;

    private static final Logger logger = LoggerFactory.getLogger(SelectiveSupervisorController.class);

    public SelectiveSupervisorController(ConsumersSupervisor supervisor,
                                         SubscriptionsCache subscriptionsCache,
                                         WorkTracker workTracker,
                                         ConsumerNodesRegistry consumersRegistry,
                                         ZookeeperAdminCache adminCache,
                                         ConfigFactory configFactory,
                                         HermesMetrics metrics) {

        this.supervisor = supervisor;
        this.subscriptionsCache = subscriptionsCache;
        this.workTracker = workTracker;
        this.consumersRegistry = consumersRegistry;
        this.adminCache = adminCache;
        this.configFactory = configFactory;
        this.metrics = metrics;
    }

    @Override
    public void onSubscriptionAssigned(Subscription subscription) {
        logger.info("Assigning consumer for {}", subscription.getId());
        supervisor.assignConsumerForSubscription(subscription);
    }

    @Override
    public void onAssignmentRemoved(SubscriptionName subscription) {
        logger.info("Removing assignment from consumer for {}", subscription.getId());
        supervisor.deleteConsumerForSubscriptionName(subscription);
    }

    @Override
    public void onSubscriptionChanged(Subscription subscription) {
        logger.info("Updating subscription {}", subscription.getId());
        supervisor.updateSubscription(subscription);
    }

    @Override
    public void start() throws Exception {
        adminCache.start();
        adminCache.addCallback(this);
        subscriptionsCache.start(Collections.singleton(this));
        workTracker.start(ImmutableList.of(this));
        supervisor.start();
        consumersRegistry.start();
        consumersRegistry.registerLeaderLatchListener(new BalancingJob(
                consumersRegistry,
                subscriptionsCache,
                new SelectiveWorkBalancer(configFactory.getIntProperty(CONSUMER_WORKLOAD_CONSUMERS_PER_SUBSCRIPTION),
                        configFactory.getIntProperty(CONSUMER_WORKLOAD_MAX_SUBSCRIPTIONS_PER_CONSUMER)),
                workTracker, metrics,
                configFactory.getIntProperty(CONSUMER_WORKLOAD_REBALANCE_INTERVAL),
                configFactory.getStringProperty(KAFKA_CLUSTER_NAME)));
        logger.info("Consumer boot complete. Workload config: [{}]",
                configFactory.print(
                        CONSUMER_WORKLOAD_NODE_ID,
                        CONSUMER_WORKLOAD_ALGORITHM,
                        CONSUMER_WORKLOAD_REBALANCE_INTERVAL,
                        CONSUMER_WORKLOAD_CONSUMERS_PER_SUBSCRIPTION,
                        CONSUMER_WORKLOAD_MAX_SUBSCRIPTIONS_PER_CONSUMER));
    }

    @Override
    public void shutdown() throws InterruptedException {
        supervisor.shutdown();
    }

    public String getId() {
        return consumersRegistry.getId();
    }

    public boolean isLeader() {
        return consumersRegistry.isLeader();
    }

    @Override
    public void onRetransmissionStarts(SubscriptionName subscription) throws Exception {
        if (workTracker.isAssignedTo(subscription, getId())) {
            supervisor.retransmit(subscription);
        }
    }

    @Override
    public void restartConsumer(SubscriptionName subscription) throws Exception {
        if (workTracker.isAssignedTo(subscription, getId())) {
            supervisor.restartConsumer(subscription);
        }
    }


}
