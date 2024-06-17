package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.di.factories.ModelAwareZookeeperNotifyingCacheFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.config.CommonConsumerProperties;
import pl.allegro.tech.hermes.consumers.config.KafkaProperties;
import pl.allegro.tech.hermes.consumers.config.SubscriptionConfiguration;
import pl.allegro.tech.hermes.consumers.config.WorkloadProperties;
import pl.allegro.tech.hermes.consumers.config.ZookeeperProperties;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.health.ConsumerMonitor;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistryPaths;
import pl.allegro.tech.hermes.consumers.subscription.cache.NotificationsBasedSubscriptionCache;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.subscription.id.ZookeeperSubscriptionIdProvider;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerFactory;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersExecutorService;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.NonblockingConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.monitor.ConsumersRuntimeMonitor;
import pl.allegro.tech.hermes.consumers.supervisor.process.Retransmitter;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.SelectiveWorkBalancer;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperGroupRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperWorkloadConstraintsRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.ModelAwareZookeeperNotifyingCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.notifications.ZookeeperInternalNotificationBus;
import pl.allegro.tech.hermes.test.helper.metrics.TestMetricsFacadeFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

class ConsumerTestRuntimeEnvironment {

    private static final int DEATH_OF_CONSUMER_AFTER_SECONDS = 300;
    private final KafkaProperties kafkaProperties = new KafkaProperties();

    private final ConsumerNodesRegistryPaths nodesRegistryPaths;

    private int consumerIdSequence = 0;

    private int subscriptionIdSequence = 0;

    private final ZookeeperPaths zookeeperPaths = new ZookeeperPaths("/hermes");
    private final Supplier<CuratorFramework> curatorSupplier;

    private final GroupRepository groupRepository;
    private final TopicRepository topicRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Supplier<MetricsFacade> metricsSupplier;
    private final WorkloadConstraintsRepository workloadConstraintsRepository;
    private final CuratorFramework curator;
    private final ConsumerPartitionAssignmentState partitionAssignmentState;
    private final ZookeeperProperties zookeeperProperties;

    private final Map<String, CuratorFramework> consumerZookeeperConnections = Maps.newHashMap();
    private final List<SubscriptionsCache> subscriptionsCaches = new ArrayList<>();

    ConsumerTestRuntimeEnvironment(Supplier<CuratorFramework> curatorSupplier) {
        this.curatorSupplier = curatorSupplier;
        this.curator = curatorSupplier.get();
        this.partitionAssignmentState = new ConsumerPartitionAssignmentState();
        this.groupRepository = new ZookeeperGroupRepository(curator, objectMapper, zookeeperPaths);
        this.topicRepository = new ZookeeperTopicRepository(curator, objectMapper, zookeeperPaths, groupRepository);
        this.subscriptionRepository = new ZookeeperSubscriptionRepository(
                curator, objectMapper, zookeeperPaths, topicRepository
        );

        this.workloadConstraintsRepository = new ZookeeperWorkloadConstraintsRepository(curator, objectMapper, zookeeperPaths);

        this.metricsSupplier = TestMetricsFacadeFactory::create;
        this.nodesRegistryPaths = new ConsumerNodesRegistryPaths(zookeeperPaths, kafkaProperties.getClusterName());
        this.zookeeperProperties = new ZookeeperProperties();
    }

    WorkloadSupervisor findLeader(List<WorkloadSupervisor> supervisors) {
        return supervisors.stream().filter(WorkloadSupervisor::isLeader).findAny().get();
    }

    private WorkloadSupervisor createConsumer(String consumerId,
                                              ConsumersSupervisor consumersSupervisor) {
        CuratorFramework curator = curatorSupplier.get();
        consumerZookeeperConnections.put(consumerId, curator);
        ConsumerNodesRegistry nodesRegistry = new ConsumerNodesRegistry(
                curator,
                executorService,
                nodesRegistryPaths,
                consumerId,
                DEATH_OF_CONSUMER_AFTER_SECONDS,
                Clock.systemDefaultZone());

        try {
            nodesRegistry.start();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        WorkloadProperties workloadProperties = new WorkloadProperties();
        workloadProperties.setNodeId(consumerId);
        workloadProperties.setRebalanceInterval(Duration.ofSeconds(1));
        workloadProperties.setConsumersPerSubscription(2);
        workloadProperties.setMonitorScanInterval(Duration.ofSeconds(1));

        ModelAwareZookeeperNotifyingCache modelAwareCache = new ModelAwareZookeeperNotifyingCacheFactory(
                curator, metricsSupplier.get(), zookeeperProperties
        ).provide();

        InternalNotificationsBus notificationsBus =
                new ZookeeperInternalNotificationBus(objectMapper, modelAwareCache);

        SubscriptionsCache subscriptionsCache = new NotificationsBasedSubscriptionCache(
                notificationsBus, groupRepository, topicRepository, subscriptionRepository
        );
        subscriptionsCaches.add(subscriptionsCache);

        SubscriptionConfiguration subscriptionConfiguration = new SubscriptionConfiguration();
        SubscriptionIds subscriptionIds = subscriptionConfiguration.subscriptionIds(notificationsBus, subscriptionsCache,
                new ZookeeperSubscriptionIdProvider(curator, zookeeperPaths), new CommonConsumerProperties());

        ConsumerAssignmentCache consumerAssignmentCache =
                new ConsumerAssignmentCache(
                        curator,
                        workloadProperties.getNodeId(),
                        kafkaProperties.getClusterName(),
                        zookeeperPaths,
                        subscriptionIds
                );
        try {
            consumerAssignmentCache.start();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        ClusterAssignmentCache clusterAssignmentCache =
                new ClusterAssignmentCache(
                        curator,
                        kafkaProperties.getClusterName(),
                        zookeeperPaths,
                        subscriptionIds,
                        nodesRegistry
                );

        ConsumerAssignmentRegistry consumerAssignmentRegistry =
                new ConsumerAssignmentRegistry(
                        curator,
                        workloadProperties.getRegistryBinaryEncoderAssignmentsBufferSizeBytes(),
                        kafkaProperties.getClusterName(),
                        zookeeperPaths,
                        subscriptionIds
                );

        return new WorkloadSupervisor(
                consumersSupervisor,
                notificationsBus,
                subscriptionsCache,
                consumerAssignmentCache,
                consumerAssignmentRegistry,
                clusterAssignmentCache,
                nodesRegistry,
                mock(ZookeeperAdminCache.class),
                executorService,
                workloadProperties,
                kafkaProperties.getClusterName(),
                metricsSupplier.get(),
                workloadConstraintsRepository,
                new SelectiveWorkBalancer(),
                new NoOpBalancingListener()
        );
    }

    WorkloadSupervisor spawnConsumer() {
        return spawnConsumers(1).get(0);
    }

    WorkloadSupervisor spawnConsumer(String consumerId, ConsumersSupervisor consumersSupervisor) {
        WorkloadSupervisor workloadSupervisor = createConsumer(consumerId, consumersSupervisor);
        return startNode(workloadSupervisor);
    }

    ConsumersSupervisor consumersSupervisor(ConsumerFactory consumerFactory) {
        MetricsFacade metrics = metricsSupplier.get();
        CommonConsumerProperties commonConsumerProperties = new CommonConsumerProperties();
        CommonConsumerProperties.BackgroundSupervisor supervisorParameters = new CommonConsumerProperties.BackgroundSupervisor();
        supervisorParameters.setInterval(Duration.ofSeconds(1));
        commonConsumerProperties.setBackgroundSupervisor(supervisorParameters);
        return new NonblockingConsumersSupervisor(commonConsumerProperties,
                new ConsumersExecutorService(new CommonConsumerProperties().getThreadPoolSize(), metrics),
                consumerFactory,
                mock(OffsetQueue.class),
                partitionAssignmentState,
                mock(Retransmitter.class),
                mock(UndeliveredMessageLogPersister.class),
                subscriptionRepository,
                metrics,
                mock(ConsumerMonitor.class),
                Clock.systemDefaultZone(),
                Duration.ofSeconds(60));
    }

    ConsumersRuntimeMonitor monitor(String consumerId,
                                    ConsumersSupervisor consumersSupervisor,
                                    WorkloadSupervisor workloadSupervisor,
                                    Duration monitorScanInterval) {
        CuratorFramework curator = consumerZookeeperConnections.get(consumerId);
        ModelAwareZookeeperNotifyingCache modelAwareCache =
                new ModelAwareZookeeperNotifyingCacheFactory(curator, metricsSupplier.get(), zookeeperProperties).provide();
        InternalNotificationsBus notificationsBus =
                new ZookeeperInternalNotificationBus(objectMapper, modelAwareCache);
        SubscriptionsCache subscriptionsCache = new NotificationsBasedSubscriptionCache(
                notificationsBus, groupRepository, topicRepository, subscriptionRepository);
        subscriptionsCaches.add(subscriptionsCache);
        subscriptionsCache.start();
        return new ConsumersRuntimeMonitor(
                consumersSupervisor,
                workloadSupervisor,
                metricsSupplier.get(),
                subscriptionsCache,
                monitorScanInterval);
    }

    List<WorkloadSupervisor> spawnConsumers(int howMany) {
        return IntStream.range(0, howMany)
                .mapToObj(i -> {
                    String consumerId = nextConsumerId();
                    ConsumersSupervisor consumersSupervisor = consumersSupervisor(mock(ConsumerFactory.class));
                    WorkloadSupervisor consumer = createConsumer(consumerId, consumersSupervisor);
                    startNode(consumer);
                    return consumer;
                })
                .collect(toList());
    }

    void kill(WorkloadSupervisor node) {
        consumerZookeeperConnections.get(node.consumerId()).close();
    }

    void cleanState() {
        consumerZookeeperConnections.values().forEach(CuratorFramework::close);
        subscriptionsCaches.clear();
    }

    private WorkloadSupervisor startNode(WorkloadSupervisor workloadSupervisor) {
        try {
            workloadSupervisor.start();
            waitForRegistration(workloadSupervisor.consumerId());
            return workloadSupervisor;
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    void waitForRegistration(String consumerId) {
        await().atMost(adjust(Duration.ofSeconds(1))).until(() -> isRegistered(consumerId));
    }

    private boolean isRegistered(String nodeId) {
        try {
            return curator.checkExists().forPath(nodesRegistryPaths.nodePath(nodeId)) != null;
        } catch (Exception e) {
            return false;
        }
    }

    void awaitUntilAssignmentExists(SubscriptionName subscription, WorkloadSupervisor node) {
        await().atMost(adjust(Duration.ofSeconds(2))).until(() -> node.assignedSubscriptions().contains(subscription));
    }

    List<SubscriptionName> createSubscription(int howMany) {
        return IntStream.range(0, howMany).mapToObj(i ->
                createSubscription(nextSubscriptionName())).collect(toList());
    }

    SubscriptionName createSubscription() {
        return createSubscription(1).get(0);
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
        await().atMost(adjust(Duration.ofSeconds(2))).untilAsserted(
                () -> {
                    assertThat(subscriptionRepository.subscriptionExists(subscription.getTopicName(), subscription.getName())).isTrue();
                    subscriptionsCaches.forEach(subscriptionsCache ->
                            assertThat(subscriptionsCache.listActiveSubscriptionNames().contains(subscriptionName)).isTrue());
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
}
