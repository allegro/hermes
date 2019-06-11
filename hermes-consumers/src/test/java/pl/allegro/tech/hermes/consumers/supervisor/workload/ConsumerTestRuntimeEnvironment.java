package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.di.factories.ModelAwareZookeeperNotifyingCacheFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.health.ConsumerMonitor;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;
import pl.allegro.tech.hermes.consumers.subscription.cache.NotificationsBasedSubscriptionCache;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerFactory;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersExecutorService;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.NonblockingConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.monitor.ConsumersRuntimeMonitor;
import pl.allegro.tech.hermes.consumers.supervisor.process.Retransmitter;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.SelectiveSupervisorController;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperGroupRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.ModelAwareZookeeperNotifyingCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.notifications.ZookeeperInternalNotificationBus;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_SECOND;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_CONSUMERS_PER_SUBSCRIPTION;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_REBALANCE_INTERVAL;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

class ConsumerTestRuntimeEnvironment {

    private static final int DEATH_OF_CONSUMER_AFTER_SECONDS = 300;
    private final static String CLUSTER_NAME = "primary_dc";

    private int consumerIdSequence = 0;

    private int subscriptionIdSequence = 0;

    private final ZookeeperPaths paths;
    private final Supplier<CuratorFramework> curatorSupplier;

    private GroupRepository groupRepository;
    private TopicRepository topicRepository;
    private SubscriptionRepository subscriptionRepository;
    private MutableConfigFactory configFactory;
    private ObjectMapper objectMapper = new ObjectMapper();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Supplier<HermesMetrics> metricsSupplier;
    private ConsumerNodesRegistry consumersRegistry;
    private CuratorFramework curator;
    private final ConsumerPartitionAssignmentState partitionAssignmentState;

    private Map<String, CuratorFramework> consumerZookeeperConnections = Maps.newHashMap();
    private List<SubscriptionsCache> subscriptionsCaches = new ArrayList<>();

    ConsumerTestRuntimeEnvironment(Supplier<CuratorFramework> curatorSupplier) {
        this.paths = new ZookeeperPaths("/hermes");
        this.curatorSupplier = curatorSupplier;
        this.curator = curatorSupplier.get();
        this.partitionAssignmentState = new ConsumerPartitionAssignmentState();
        this.groupRepository = new ZookeeperGroupRepository(curator, objectMapper, paths);
        this.topicRepository = new ZookeeperTopicRepository(curator, objectMapper, paths, groupRepository);
        this.subscriptionRepository = new ZookeeperSubscriptionRepository(
                curator, objectMapper, paths, topicRepository
        );

        this.configFactory = new MutableConfigFactory()
                .overrideProperty(CONSUMER_WORKLOAD_REBALANCE_INTERVAL, 1)
                .overrideProperty(CONSUMER_WORKLOAD_CONSUMERS_PER_SUBSCRIPTION, 2);

        this.consumersRegistry = new ConsumerNodesRegistry(
                curator, executorService, paths.consumersRegistryPath(CLUSTER_NAME), "id",
                DEATH_OF_CONSUMER_AFTER_SECONDS, Clock.systemDefaultZone());

        this.metricsSupplier = () -> new HermesMetrics(new MetricRegistry(), new PathsCompiler("localhost"));

        try {
            consumersRegistry.start();
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    SelectiveSupervisorController findLeader(List<SelectiveSupervisorController> supervisors) {
        return supervisors.stream().filter(SelectiveSupervisorController::isLeader).findAny().get();
    }

    private ConsumerControllers createConsumer(String consumerId) {
        return createConsumer(consumerId, consumersSupervisor(mock(ConsumerFactory.class)));
    }

    private ConsumerControllers createConsumer(String consumerId,
                                                         ConsumersSupervisor consumersSupervisor) {
        CuratorFramework curator = curatorSupplier.get();
        consumerZookeeperConnections.put(consumerId, curator);
        ConsumerNodesRegistry registry = new ConsumerNodesRegistry(
                curator,
                executorService,
                paths.consumersRegistryPath(CLUSTER_NAME),
                consumerId,
                DEATH_OF_CONSUMER_AFTER_SECONDS,
                Clock.systemDefaultZone());

        ModelAwareZookeeperNotifyingCache modelAwareCache = new ModelAwareZookeeperNotifyingCacheFactory(
                curator, configFactory
        ).provide();
        InternalNotificationsBus notificationsBus =
                new ZookeeperInternalNotificationBus(objectMapper, modelAwareCache);
        SubscriptionsCache subscriptionsCache = new NotificationsBasedSubscriptionCache(
                notificationsBus, groupRepository, topicRepository, subscriptionRepository
        );
        subscriptionsCaches.add(subscriptionsCache);

        SubscriptionAssignmentCache subscriptionAssignmentCache = new SubscriptionAssignmentCache(
                curator, configFactory, paths, subscriptionsCache
        );

        SubscriptionAssignmentRegistry assignmentRegistry = new SubscriptionAssignmentRegistryFactory(
                curator, configFactory, subscriptionAssignmentCache
        ).provide();

        WorkTracker workTracker = new WorkTracker(consumerId, assignmentRegistry);

        SelectiveSupervisorController supervisor = new SelectiveSupervisorController(
                consumersSupervisor, notificationsBus, subscriptionsCache, assignmentRegistry, workTracker, registry,
                mock(ZookeeperAdminCache.class), executorService, configFactory, metricsSupplier.get()
        );

        return new ConsumerControllers(subscriptionAssignmentCache, supervisor);
    }

    SelectiveSupervisorController spawnConsumer(String consumerId, ConsumersSupervisor consumersSupervisor) {
        ConsumerControllers consumerControllers = createConsumer(consumerId, consumersSupervisor);
        return startNode(consumerControllers).supervisorController;
    }

    ConsumersSupervisor consumersSupervisor(ConsumerFactory consumerFactory) {
        HermesMetrics metrics = metricsSupplier.get();
        return new NonblockingConsumersSupervisor(configFactory,
                new ConsumersExecutorService(configFactory, metrics),
                consumerFactory,
                mock(OffsetQueue.class),
                partitionAssignmentState,
                mock(Retransmitter.class),
                mock(UndeliveredMessageLogPersister.class),
                subscriptionRepository,
                metrics,
                mock(ConsumerMonitor.class),
                Clock.systemDefaultZone());
    }

    ConsumersRuntimeMonitor monitor(String consumerId,
                                    ConsumersSupervisor consumersSupervisor,
                                    SupervisorController supervisorController) {
        CuratorFramework curator = consumerZookeeperConnections.get(consumerId);
        ModelAwareZookeeperNotifyingCache modelAwareCache =
                new ModelAwareZookeeperNotifyingCacheFactory(curator, configFactory).provide();
        InternalNotificationsBus notificationsBus =
                new ZookeeperInternalNotificationBus(objectMapper, modelAwareCache);
        SubscriptionsCache subscriptionsCache = new NotificationsBasedSubscriptionCache(
                notificationsBus, groupRepository, topicRepository, subscriptionRepository);
        subscriptionsCaches.add(subscriptionsCache);
        subscriptionsCache.start();
        return new ConsumersRuntimeMonitor(
                consumersSupervisor,
                supervisorController,
                metricsSupplier.get(),
                subscriptionsCache,
                configFactory);
    }

    private List<ConsumerControllers> createConsumers(int howMany) {
        return IntStream.range(0, howMany).mapToObj(
                i -> createConsumer(nextConsumerId())
        ).collect(toList());
    }

    List<SelectiveSupervisorController> spawnConsumers(int howMany) {
        List<ConsumerControllers> nodes = createConsumers(howMany);
        nodes.forEach(this::startNode);
        return nodes.stream().map(ConsumerControllers::getSupervisorController).collect(toList());
    }

    SelectiveSupervisorController spawnConsumer() {
        return spawnConsumers(1).get(0);
    }

    void kill(SelectiveSupervisorController node) {
        consumerZookeeperConnections.get(node.consumerId()).close();
    }

    void killAll() {
        consumerZookeeperConnections.values().stream().forEach(CuratorFramework::close);
    }

    private ConsumerControllers startNode(ConsumerControllers consumerControllers) {
        try {
            consumerControllers.supervisorController.start();
            waitForRegistration(consumerControllers.supervisorController.consumerId());
            consumerControllers.assignmentCache.start();
            return consumerControllers;
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    void waitForRegistration(String consumerId) {
        await().atMost(adjust(ONE_SECOND)).until(() -> isRegistered(consumerId));
    }

    private boolean isRegistered(String id) {
        return consumersRegistry.isRegistered(id);
    }

    void awaitUntilAssignmentExists(SubscriptionName subscription, SelectiveSupervisorController node) {
        awaitUntilAssignmentExists(subscription.toString(), node.consumerId());
    }

    void awaitUntilAssignmentExists(String subscription, String supervisorId) {
        await().atMost(adjust(ONE_SECOND)).until(
                () -> curator.checkExists().forPath(assignmentPath(subscription, supervisorId)) != null
        );
    }

    void createAssignment(SubscriptionName subscription, String consumerId) {
        try {
            curator.create().creatingParentsIfNeeded().forPath(assignmentPath(subscription.toString(), consumerId));
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private String assignmentPath(String subscription, String supervisorId) {
        return paths.consumersRuntimePath(CLUSTER_NAME) + "/" + subscription + "/" + supervisorId;
    }

    List<SubscriptionName> createSubscription(int howMany) {
        return IntStream.range(0, howMany).mapToObj(i ->
                createSubscription(nextSubscriptionName())).collect(toList());
    }

    SubscriptionName createSubscription() {
        return createSubscription(1).get(0);
    }

    Subscription getSubscription(SubscriptionName subscriptionName) {
        return subscriptionRepository.getSubscriptionDetails(subscriptionName);
    }

    private SubscriptionName createSubscription(SubscriptionName subscriptionName) {
        Subscription subscription = subscription(subscriptionName).build();
        Group group = Group.from(subscription.getTopicName().getGroupName());
        if (!groupRepository.groupExists(group.getGroupName())) {
            groupRepository.createGroup(group);
        }
        if (!topicRepository.topicExists(subscription.getTopicName())) {
            topicRepository.createTopic(topic(subscription.getTopicName()).build());
        }
        subscriptionRepository.createSubscription(subscription);
        await().atMost(adjust(ONE_SECOND)).until(
                () -> {
                    subscriptionRepository.subscriptionExists(subscription.getTopicName(), subscription.getName());
                    subscriptionsCaches.forEach(subscriptionsCache ->
                            subscriptionsCache.listActiveSubscriptionNames().contains(subscriptionName));
                }
        );
        return subscription.getQualifiedName();
    }

    private String nextConsumerId() {
        return String.valueOf(consumerIdSequence++);
    }

    private SubscriptionName nextSubscriptionName() {
        return SubscriptionName.fromString("com.example.topic$test" + subscriptionIdSequence++);
    }

    void verifyConsumerWouldBeCreated(ConsumersSupervisor supervisor, Subscription subscription) {
        await().atMost(adjust(ONE_SECOND)).until(
                () -> verify(supervisor).assignConsumerForSubscription(subscription));

    }

    void withOverriddenConfigProperty(Configs property, int value) {
        this.configFactory.overrideProperty(property, value);
    }

    static class ConsumerControllers {
        SubscriptionAssignmentCache assignmentCache;
        SelectiveSupervisorController supervisorController;

        public ConsumerControllers(SubscriptionAssignmentCache assignmentCache,
                                   SelectiveSupervisorController supervisorController) {
            this.assignmentCache = assignmentCache;
            this.supervisorController = supervisorController;
        }


        public SelectiveSupervisorController getSupervisorController() {
            return supervisorController;
        }
    }
}
