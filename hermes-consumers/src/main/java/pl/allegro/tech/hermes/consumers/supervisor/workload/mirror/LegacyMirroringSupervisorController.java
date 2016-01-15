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

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_ALGORITHM;

public class LegacyMirroringSupervisorController implements SupervisorController {
    private final ConsumersSupervisor supervisor;
    private final SubscriptionsCache subscriptionsCache;
    private final ZookeeperAdminCache adminCache;
    private ConfigFactory configFactory;

    private static final Logger logger = LoggerFactory.getLogger(LegacyMirroringSupervisorController.class);

    public LegacyMirroringSupervisorController(ConsumersSupervisor supervisor,
                                               SubscriptionsCache subscriptionsCache,
                                               ZookeeperAdminCache adminCache,
                                               ConfigFactory configFactory) {
        this.supervisor = supervisor;
        this.subscriptionsCache = subscriptionsCache;
        this.adminCache = adminCache;
        this.configFactory = configFactory;
    }

    @Override
    public void onSubscriptionCreated(Subscription subscription) {
        supervisor.assignConsumerForSubscription(subscription);
    }

    @Override
    public void onSubscriptionRemoved(Subscription subscription) {
        supervisor.deleteConsumerForSubscriptionName(subscription.toSubscriptionName());
    }

    @Override
    public void onSubscriptionChanged(Subscription subscription) {
        supervisor.notifyConsumerOnSubscriptionUpdate(subscription);
    }

    @Override
    public void start() throws Exception {
        adminCache.start();
        adminCache.addCallback(this);
        subscriptionsCache.start(ImmutableList.of(this));
        supervisor.start();
        logger.info("Consumer boot complete. Workload config: [{}]", configFactory.print(CONSUMER_WORKLOAD_ALGORITHM));
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
