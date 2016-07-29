package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.di.factories.ModelAwareZookeeperNotifyingCacheFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.subscription.cache.NotificationsBasedSubscriptionCache;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_SECOND;
import static org.mockito.Mockito.mock;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_REBALANCE_INTERVAL;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

class ConsumerTestRuntimeEnvironment {

    private final static String CLUSTER_NAME = "primary";

    private int consumerIdSequence = 0;

    private int subscriptionIdSequence = 0;

    private final ZookeeperPaths paths;
    private final Supplier<CuratorFramework> curatorSupplier;

    private GroupRepository groupRepository;
    private TopicRepository topicRepository;
    private SubscriptionRepository subscriptionRepository;
    private MutableConfigFactory configFactory;
    private ObjectMapper objectMapper = new ObjectMapper();
    private ConsumersSupervisor supervisor = mock(ConsumersSupervisor.class);
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private HermesMetrics metrics;
    private ConsumerNodesRegistry consumersRegistry;
    private CuratorFramework curator;

    private Map<String, CuratorFramework> consumerZookeeperConnections = Maps.newHashMap();

    ConsumerTestRuntimeEnvironment(Supplier<CuratorFramework> curatorSupplier) {
        this.paths = new ZookeeperPaths("/hermes");
        this.curatorSupplier = curatorSupplier;
        this.curator = curatorSupplier.get();
        this.groupRepository = new ZookeeperGroupRepository(curator, objectMapper, paths);
        this.topicRepository = new ZookeeperTopicRepository(curator, objectMapper, paths, groupRepository);
        this.subscriptionRepository = new ZookeeperSubscriptionRepository(
                curator, objectMapper, paths, topicRepository
        );

        this.configFactory = new MutableConfigFactory().overrideProperty(CONSUMER_WORKLOAD_REBALANCE_INTERVAL, 1);

        this.consumersRegistry = new ConsumerNodesRegistry(
                curator, executorService, paths.consumersRegistryPath(CLUSTER_NAME), "id"
        );

        this.metrics = new HermesMetrics(new MetricRegistry(), new PathsCompiler("localhost"));

        try {
            consumersRegistry.start();
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    SelectiveSupervisorController findLeader(List<SelectiveSupervisorController> supervisors) {
        return supervisors.stream().filter(SelectiveSupervisorController::isLeader).findAny().get();
    }

    private SelectiveSupervisorController createConsumer(String consumerId) {
        CuratorFramework curator = curatorSupplier.get();
        consumerZookeeperConnections.put(consumerId, curator);
        ConsumerNodesRegistry registry = new ConsumerNodesRegistry(
                curator,
                executorService,
                paths.consumersRegistryPath(CLUSTER_NAME),
                consumerId
        );

        ModelAwareZookeeperNotifyingCache modelAwareCache = new ModelAwareZookeeperNotifyingCacheFactory(
                curator, configFactory
        ).provide();
        InternalNotificationsBus notificationsBus = new ZookeeperInternalNotificationBus(objectMapper, modelAwareCache);
        SubscriptionsCache subscriptionsCache = new NotificationsBasedSubscriptionCache(
                notificationsBus, groupRepository, topicRepository, subscriptionRepository
        );
        SubscriptionAssignmentRegistry assignmentRegistry = new SubscriptionAssignmentRegistryFactory(
                curator, configFactory, subscriptionsCache
        ).provide();

        WorkTracker workTracker = new WorkTracker(consumerId, assignmentRegistry);

        return new SelectiveSupervisorController(
                supervisor, notificationsBus, subscriptionsCache, assignmentRegistry, workTracker, registry,
                mock(ZookeeperAdminCache.class), executorService, configFactory, metrics
        );
    }

    private List<SelectiveSupervisorController> createConsumers(int howMany) {
        return IntStream.range(0, howMany).mapToObj(
                i -> createConsumer(nextConsumerId())
        ).collect(Collectors.toList());
    }

    List<SelectiveSupervisorController> spawnConsumers(int howMany) {
        List<SelectiveSupervisorController> nodes = createConsumers(howMany);
        nodes.forEach(this::startNode);
        return nodes;
    }

    SelectiveSupervisorController spawnConsumer() {
        return spawnConsumers(1).get(0);
    }

    void kill(SelectiveSupervisorController node) {
        consumerZookeeperConnections.get(node.getId()).close();
    }

    void killAll() {
        consumerZookeeperConnections.values().stream().forEach(CuratorFramework::close);
    }

    private SelectiveSupervisorController startNode(SelectiveSupervisorController supervisorController) {
        try {
            supervisorController.start();
            waitForRegistration(supervisorController.getId());
            return supervisorController;
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
        awaitUntilAssignmentExists(subscription.toString(), node.getId());
    }

    void awaitUntilAssignmentExists(String subscription, String supervisorId) {
        await().atMost(adjust(ONE_SECOND)).until(
                () -> curator.checkExists().forPath(assignmentPath(subscription, supervisorId)) != null
        );
    }

    private String assignmentPath(String subscription, String supervisorId) {
        return paths.consumersRuntimePath(CLUSTER_NAME) + "/" + subscription + "/" + supervisorId;
    }

    List<SubscriptionName> createSubscription(int howMany) {
        return IntStream.range(0, howMany).mapToObj(i ->
                createSubscription(nextSubscriptionName())).collect(Collectors.toList());
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
        await().atMost(adjust(ONE_SECOND)).until(
                () -> subscriptionRepository.subscriptionExists(subscription.getTopicName(), subscription.getName())
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
