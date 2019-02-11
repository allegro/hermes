package pl.allegro.tech.hermes.consumers.supervisor.workload.mirror;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorController;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkTracker;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_ALGORITHM;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_NODE_ID;

public class MirroringSupervisorController implements SupervisorController {

    private static final Logger logger = LoggerFactory.getLogger(MirroringSupervisorController.class);

    private final String consumerNodeId;

    private final ConsumersSupervisor supervisor;
    private final InternalNotificationsBus notificationsBus;
    private final SubscriptionAssignmentRegistry assignmentRegistry;
    private final SubscriptionsCache subscriptionsCache;
    private final WorkTracker workTracker;
    private final ZookeeperAdminCache adminCache;
    private final ConfigFactory configFactory;

    private final ExecutorService executorService;

    public MirroringSupervisorController(ConsumersSupervisor supervisor,
                                         InternalNotificationsBus notificationsBus,
                                         SubscriptionAssignmentRegistry assignmentRegistry,
                                         SubscriptionsCache subscriptionsCache,
                                         WorkTracker workTracker,
                                         ZookeeperAdminCache adminCache,
                                         ConfigFactory configFactory) {
        this.consumerNodeId = configFactory.getStringProperty(CONSUMER_WORKLOAD_NODE_ID);

        this.supervisor = supervisor;
        this.notificationsBus = notificationsBus;
        this.assignmentRegistry = assignmentRegistry;
        this.subscriptionsCache = subscriptionsCache;
        this.workTracker = workTracker;
        this.adminCache = adminCache;
        this.configFactory = configFactory;
        this.executorService = Executors.newFixedThreadPool(
                configFactory.getIntProperty(Configs.ZOOKEEPER_TASK_PROCESSING_THREAD_POOL_SIZE),
                new ThreadFactoryBuilder().setNameFormat("mirroring-supervisor-%d").build()
        );
    }

    @Override
    public void onSubscriptionCreated(Subscription subscription) {
        executorService.submit(() -> workTracker.forceAssignment(subscription));
    }

    @Override
    public void onSubscriptionRemoved(Subscription subscription) {
        executorService.submit(() -> workTracker.dropAssignment(subscription));
    }

    @Override
    public void onSubscriptionChanged(Subscription subscription) {
        executorService.submit(() -> {
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
        });
    }

    @Override
    public void onTopicChanged(Topic topic) {
        for (Subscription subscription : subscriptionsCache.subscriptionsOfTopic(topic.getName())) {
            executorService.submit(() -> supervisor.updateTopic(subscription, topic));
        }
    }

    @Override
    public void onSubscriptionAssigned(SubscriptionName subscriptionName) {
        logger.info("Assigning consumer for {}", subscriptionName.getQualifiedName());
        Subscription subscription = subscriptionsCache.getSubscription(subscriptionName);
        supervisor.assignConsumerForSubscription(subscription);
    }

    @Override
    public void onAssignmentRemoved(SubscriptionName subscription) {
        logger.info("Removing assignment from consumer for {}", subscription);
        supervisor.deleteConsumerForSubscriptionName(subscription);
    }

    @Override
    public void start() throws Exception {
        adminCache.start();
        adminCache.addCallback(this);

        notificationsBus.registerSubscriptionCallback(this);
        notificationsBus.registerTopicCallback(this);
        assignmentRegistry.registerAssignmentCallback(this);

        supervisor.start();
        assignmentRegistry.start();
        logger.info("Consumer boot complete. Workload config: [{}]", configFactory.print(CONSUMER_WORKLOAD_NODE_ID, CONSUMER_WORKLOAD_ALGORITHM));
    }

    @Override
    public Set<SubscriptionName> assignedSubscriptions() {
        return assignmentRegistry.createSnapshot().getSubscriptionsForConsumerNode(consumerNodeId);
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
    public Optional<String> watchedConsumerId() {
        return Optional.of(consumerNodeId);
    }
}
