package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.subscription.cache.zookeeper.ZookeeperSubscriptionsCacheFactory;
import pl.allegro.tech.hermes.consumers.supervisor.LegacyConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.SelectiveSupervisorController;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperGroupRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository;
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
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_REBALANCE_INTERVAL;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class ConsumerTestRuntimeEnvironment {
    private final static String CLUSTER_NAME = "primary";
    private final ZookeeperPaths paths;
    private final Supplier<CuratorFramework> curatorSupplier;
    private GroupRepository groupRepository;
    private TopicRepository topicRepository;
    private SubscriptionRepository subscriptionRepository;
    private MutableConfigFactory configFactory;
    private ObjectMapper objectMapper = new ObjectMapper();
    private LegacyConsumersSupervisor supervisor = mock(LegacyConsumersSupervisor.class);
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();
    private HermesMetrics metrics;
    private ConsumerNodesRegistry consumersRegistry;
    private CuratorFramework curator;
    private Map<String, CuratorFramework> curators = Maps.newHashMap();

    public ConsumerTestRuntimeEnvironment(Supplier<CuratorFramework> curatorSupplier) {
        this.paths = new ZookeeperPaths("/hermes");
        this.curatorSupplier = curatorSupplier;
        this.curator = curatorSupplier.get();
        this.groupRepository = new ZookeeperGroupRepository(curator, objectMapper, paths);
        this.topicRepository = new ZookeeperTopicRepository(curator, objectMapper, paths, groupRepository);
        this.subscriptionRepository = new ZookeeperSubscriptionRepository(curator, objectMapper, paths, topicRepository);
        this.configFactory = new MutableConfigFactory().overrideProperty(CONSUMER_WORKLOAD_REBALANCE_INTERVAL, 1);
        this.consumersRegistry = new ConsumerNodesRegistry(curator, executorService, paths.consumersRegistryPath(CLUSTER_NAME), "id");
        this.metrics = mock(HermesMetrics.class);
        when(metrics.consumersWorkloadRebalanceDurationTimer(anyString())).thenReturn(new Timer());
        try {
            curator.create().creatingParentsIfNeeded().forPath("/hermes/groups");
            consumersRegistry.start();
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public SubscriptionName createSubscription(String subscriptionName) {
        Subscription subscription = subscription(SubscriptionName.fromString(subscriptionName)).build();
        Group group = Group.from(subscription.getTopicName().getGroupName());
        if (!groupRepository.groupExists(group.getGroupName())) {
            groupRepository.createGroup(group);
        }
        if (!topicRepository.topicExists(subscription.getTopicName())) {
            topicRepository.createTopic(topic(subscription.getTopicName()).build());
        }
        subscriptionRepository.createSubscription(subscription);
        await().atMost(adjust(ONE_SECOND)).until(() -> subscriptionRepository.subscriptionExists(subscription.getTopicName(), subscription.getName()));
        return subscription.toSubscriptionName();
    }

    public SelectiveSupervisorController findLeader(List<SelectiveSupervisorController> supervisors) {
        return supervisors.stream()
                .filter(SelectiveSupervisorController::isLeader).findAny().get();
    }

    public SelectiveSupervisorController node(String id, CuratorFramework curator) {
        curators.put(id, curator);
        WorkTracker workTracker = new WorkTracker(curator, objectMapper, paths.consumersRuntimePath(CLUSTER_NAME), id, executorService, subscriptionRepository);
        ConsumerNodesRegistry registry = new ConsumerNodesRegistry(curator, executorService, paths.consumersRegistryPath(CLUSTER_NAME), id);
        SubscriptionsCache subscriptionsCache = new ZookeeperSubscriptionsCacheFactory(curator, configFactory, objectMapper).provide();
        return new SelectiveSupervisorController(supervisor, subscriptionsCache, workTracker, registry, mock(ZookeeperAdminCache.class), executorService, configFactory, metrics);
    }

    public SelectiveSupervisorController node(String id) {
        return node(id, curatorSupplier.get());
    }

    public List<SelectiveSupervisorController> nodes(int howMany) {
        return IntStream.range(0, howMany).mapToObj(i ->
                node(Integer.toString(i))).collect(Collectors.toList());
    }

    public boolean isRegistered(String id) {
        return consumersRegistry.isRegistered(id);
    }

    public void awaitUntilAssignmentExists(String subscription, String supervisorId) {
        await().atMost(adjust(ONE_SECOND)).until(() -> curator.checkExists().forPath(assignmentPath(subscription, supervisorId)) != null);
    }

    private String assignmentPath(String subscription, String supervisorId) {
        return paths.consumersRuntimePath(CLUSTER_NAME) + "/" + subscription + "/" + supervisorId;
    }

    public void waitForRegistration(String id) {
        await().atMost(adjust(ONE_SECOND)).until(() -> isRegistered(id));
    }

    public SelectiveSupervisorController startNode(SelectiveSupervisorController supervisorController) {
        try {
            supervisorController.start();
            waitForRegistration(supervisorController.getId());
            return supervisorController;
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public SelectiveSupervisorController spawnNode() {
        return startNode(node(Integer.toString(nextInt())));
    }

    public void awaitUntilAssignmentExists(SubscriptionName subscription, SelectiveSupervisorController node) {
        awaitUntilAssignmentExists(subscription.toString(), node.getId());
    }

    public List<SelectiveSupervisorController> spawnNodes(int howMany) {
        List<SelectiveSupervisorController> nodes = nodes(howMany);
        nodes.forEach(this::startNode);
        return nodes;
    }

    public List<SubscriptionName> createSubscription(int howMany) {
        return IntStream.range(0, howMany).mapToObj(i ->
                createSubscription("com.example.topic$test" + i)).collect(Collectors.toList());
    }

    public void kill(SelectiveSupervisorController node) {
        curators.get(node.getId()).close();
    }
}
