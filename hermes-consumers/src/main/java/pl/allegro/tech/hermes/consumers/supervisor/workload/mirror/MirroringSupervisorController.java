package pl.allegro.tech.hermes.consumers.supervisor.workload.mirror;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorController;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkTracker;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_ALGORITHM;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_NODE_ID;

public class MirroringSupervisorController implements SupervisorController {
    private ConsumersSupervisor supervisor;
    private SubscriptionsCache subscriptionsCache;
    private WorkTracker workTracker;
    private ZookeeperAdminCache adminCache;
    private ConfigFactory configFactory;

    private static final Logger logger = LoggerFactory.getLogger(MirroringSupervisorController.class);

    public MirroringSupervisorController(ConsumersSupervisor supervisor,
                                         SubscriptionsCache subscriptionsCache,
                                         WorkTracker workTracker,
                                         ZookeeperAdminCache adminCache,
                                         ConfigFactory configFactory) {
        this.supervisor = supervisor;
        this.subscriptionsCache = subscriptionsCache;
        this.workTracker = workTracker;
        this.adminCache = adminCache;
        this.configFactory = configFactory;
    }

    @Override
    public void onSubscriptionCreated(Subscription subscription) {
        workTracker.forceAssignment(subscription);
    }

    @Override
    public void onSubscriptionRemoved(Subscription subscription) {
        workTracker.dropAssignment(subscription);
    }

    @Override
    public void onSubscriptionChanged(Subscription subscription) {
        switch (subscription.getState()) {
            case PENDING:
            case ACTIVE:
                workTracker.forceAssignment(subscription);
                break;
            case SUSPENDED:
                workTracker.dropAssignment(subscription);
                break;
            default:
                break;
        }
        supervisor.updateSubscription(subscription);
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
    public void start() throws Exception {
        adminCache.start();
        adminCache.addCallback(this);
        subscriptionsCache.start(ImmutableList.of(this));
        workTracker.start(ImmutableList.of(this));
        supervisor.start();
        logger.info("Consumer boot complete. Workload config: [{}]", configFactory.print(CONSUMER_WORKLOAD_NODE_ID, CONSUMER_WORKLOAD_ALGORITHM));
    }

    @Override
    public void shutdown() throws InterruptedException {
        supervisor.shutdown();
    }

    @Override
    public void onRetransmissionStarts(SubscriptionName subscription) throws Exception {
        supervisor.retransmit(subscription);
    }

    @Override
    public void restartConsumer(SubscriptionName subscription) throws Exception {
        supervisor.restartConsumer(subscription);
    }
}
