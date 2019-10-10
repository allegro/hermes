package pl.allegro.tech.hermes.consumers.supervisor.workload.mirror;

import com.google.common.collect.Sets;
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
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignment;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentView;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorController;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_ALGORITHM;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_NODE_ID;

public class MirroringSupervisorController implements SupervisorController {

    private static final Logger logger = LoggerFactory.getLogger(MirroringSupervisorController.class);

    private final String consumerNodeId;

    private final ConsumersSupervisor supervisor;
    private final InternalNotificationsBus notificationsBus;
    private final ConsumerAssignmentCache assignmentCache;
    private final ConsumerAssignmentRegistry consumerAssignmentRegistry;
    private final SubscriptionsCache subscriptionsCache;
    private final ZookeeperAdminCache adminCache;
    private final ConfigFactory configFactory;

    private final ExecutorService executorService;

    public MirroringSupervisorController(ConsumersSupervisor supervisor,
                                         InternalNotificationsBus notificationsBus,
                                         ConsumerAssignmentCache assignmentCache,
                                         ConsumerAssignmentRegistry consumerAssignmentRegistry,
                                         SubscriptionsCache subscriptionsCache,
                                         ZookeeperAdminCache adminCache,
                                         ConfigFactory configFactory) {
        this.consumerNodeId = configFactory.getStringProperty(CONSUMER_WORKLOAD_NODE_ID);
        this.supervisor = supervisor;
        this.notificationsBus = notificationsBus;
        this.assignmentCache = assignmentCache;
        this.consumerAssignmentRegistry = consumerAssignmentRegistry;
        this.subscriptionsCache = subscriptionsCache;
        this.adminCache = adminCache;
        this.configFactory = configFactory;
        this.executorService = Executors.newFixedThreadPool(
                configFactory.getIntProperty(Configs.ZOOKEEPER_TASK_PROCESSING_THREAD_POOL_SIZE),
                new ThreadFactoryBuilder().setNameFormat("mirroring-supervisor-%d").build()
        );
    }

    @Override
    public void onSubscriptionCreated(Subscription subscription) {
        Set<SubscriptionAssignment> currentAssignments = getConsumerAssignments();

        SubscriptionAssignmentView currentState = SubscriptionAssignmentView.of(currentAssignments);

        SubscriptionAssignment addedAssignment = new SubscriptionAssignment(consumerNodeId, subscription.getQualifiedName());
        SubscriptionAssignmentView targetState = SubscriptionAssignmentView.of(
                Sets.union(currentAssignments, Collections.singleton(addedAssignment))
        );
        executorService.submit(() -> consumerAssignmentRegistry.updateAssignments(currentState, targetState));
    }

    @Override
    public void onSubscriptionRemoved(Subscription subscription) {
        Set<SubscriptionAssignment> currentAssignments = getConsumerAssignments();

        SubscriptionAssignmentView currentState = SubscriptionAssignmentView.of(currentAssignments);

        SubscriptionAssignment deletedAssignment = new SubscriptionAssignment(consumerNodeId, subscription.getQualifiedName());
        SubscriptionAssignmentView targetState = SubscriptionAssignmentView.of(
                Sets.difference(currentAssignments, Collections.singleton(deletedAssignment))
        );

        executorService.submit(() -> consumerAssignmentRegistry.updateAssignments(currentState, targetState));
    }

    private Set<SubscriptionAssignment> getConsumerAssignments() {
        return assignmentCache.getConsumerSubscriptions().stream()
                .map(s -> new SubscriptionAssignment(consumerNodeId, s))
                .collect(Collectors.toSet());
    }

    @Override
    public void onSubscriptionChanged(Subscription subscription) {
        executorService.submit(() -> {
            switch (subscription.getState()) {
                case PENDING:
                case ACTIVE:
                    onSubscriptionCreated(subscription);
                    break;
                case SUSPENDED:
                    onSubscriptionRemoved(subscription);
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
        assignmentCache.registerAssignmentCallback(this);

        supervisor.start();
        logger.info("Consumer boot complete. Workload config: [{}]", configFactory.print(CONSUMER_WORKLOAD_NODE_ID, CONSUMER_WORKLOAD_ALGORITHM));
    }

    @Override
    public Set<SubscriptionName> assignedSubscriptions() {
        return assignmentCache.getConsumerSubscriptions();
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
