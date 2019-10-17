package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FlatBinaryWorkloadRegistryTest extends ZookeeperBaseTest {

    private static final SubscriptionName subscription1 = SubscriptionName.fromString("pl.allegro.tech.hermes$testSubscription");
    private static final SubscriptionName subscription2 = SubscriptionName.fromString("pl.allegro.tech.hermes$testSubscription2");
    private static final SubscriptionName subscription3 = SubscriptionName.fromString("pl.allegro.tech.hermes$testSubscription3");

    private static final String consumer1 = "consumer1";
    private static final String consumer2 = "consumer2";

    private static final ConfigFactory configFactory = new MutableConfigFactory();

    private static final ZookeeperPaths zookeeperPaths = new ZookeeperPaths("/hermes");

    private static final FlatBinaryWorkloadRegistryPaths registryPaths =
            new FlatBinaryWorkloadRegistryPaths(zookeeperPaths, configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME));

    private static final SubscriptionIds subscriptionIds =
            new TestSubscriptionIds(ImmutableList.of(
                    SubscriptionId.from(subscription1, 1L),
                    SubscriptionId.from(subscription2, 2L),
                    SubscriptionId.from(subscription2, 3L)
            ));

    private static final ConsumerAssignmentRegistry registry =
            new FlatBinaryConsumerAssignmentRegistry(zookeeperClient, configFactory, zookeeperPaths, subscriptionIds);

    private static final String cluster = configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME);

    private static final FlatBinaryConsumerAssignmentCache assignmentCacheOfConsumer1 =
            new FlatBinaryConsumerAssignmentCache(zookeeperClient, consumer1, cluster, zookeeperPaths, subscriptionIds);

    private static final FlatBinaryConsumerAssignmentCache assignmentCacheOfConsumer2 =
            new FlatBinaryConsumerAssignmentCache(zookeeperClient, consumer2, cluster, zookeeperPaths, subscriptionIds);

    private static final ConsumerNodesRegistry consumerNodesRegistry = mock(ConsumerNodesRegistry.class);

    private static final FlatBinaryClusterAssignmentCache clusterAssignmentCache = new FlatBinaryClusterAssignmentCache(zookeeperClient, cluster, zookeeperPaths, subscriptionIds, consumerNodesRegistry);

    @BeforeClass
    public static void setUp() throws Exception {
        assignmentCacheOfConsumer1.start();
        assignmentCacheOfConsumer2.start();
    }

    @Before
    public void beforeEach() {
        when(consumerNodesRegistry.listConsumerNodes()).thenReturn(ImmutableList.of(consumer1, consumer2));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        assignmentCacheOfConsumer1.stop();
        assignmentCacheOfConsumer2.stop();
    }

    @Test
    public void shouldSaveComputedAssignmentsAndNotifyConsumers() {
        // given
        SubscriptionAssignmentView initView = new SubscriptionAssignmentView(Collections.emptyMap());
        SubscriptionAssignmentView targetView = new SubscriptionAssignmentView(ImmutableMap.of(
                subscription1, ImmutableSet.of(
                        new SubscriptionAssignment(consumer1, subscription1),
                        new SubscriptionAssignment(consumer2, subscription1)
                ),
                subscription2, ImmutableSet.of(
                        new SubscriptionAssignment(consumer1, subscription2)
                )
        ));

        // when
        WorkDistributionChanges changes = registry.updateAssignments(initView, targetView);

        // then
        assertThat(changes.getCreatedAssignmentsCount()).isEqualTo(3);
        assertThat(changes.getDeletedAssignmentsCount()).isZero();

        wait.untilZookeeperPathIsCreated(registryPaths.consumerWorkloadPath(consumer1));
        wait.untilZookeeperPathIsCreated(registryPaths.consumerWorkloadPath(consumer2));

        // and
        assertThat(assignmentCacheOfConsumer1.isAssignedTo(subscription1)).isTrue();
        assertThat(assignmentCacheOfConsumer1.isAssignedTo(subscription2)).isTrue();
        assertThat(assignmentCacheOfConsumer1.isAssignedTo(subscription3)).isFalse();

        assertThat(assignmentCacheOfConsumer2.isAssignedTo(subscription1)).isTrue();
        assertThat(assignmentCacheOfConsumer2.isAssignedTo(subscription2)).isFalse();
    }

    @Test
    public void shouldSaveComputedAssignmentsAndReadThroughClusterAssignmentCache() {
        // given
        SubscriptionAssignmentView initView = new SubscriptionAssignmentView(Collections.emptyMap());
        SubscriptionAssignmentView targetView = new SubscriptionAssignmentView(ImmutableMap.of(
                subscription1, ImmutableSet.of(
                        new SubscriptionAssignment(consumer1, subscription1),
                        new SubscriptionAssignment(consumer2, subscription1)
                ),
                subscription2, ImmutableSet.of(
                        new SubscriptionAssignment(consumer1, subscription2)
                )
        ));

        // when
        WorkDistributionChanges changes = registry.updateAssignments(initView, targetView);

        // then
        assertThat(changes.getCreatedAssignmentsCount()).isEqualTo(3);
        assertThat(changes.getDeletedAssignmentsCount()).isZero();

        // when
        clusterAssignmentCache.refresh();

        // then
        assertThat(clusterAssignmentCache.getAssignedConsumers()).containsOnly(consumer1, consumer2);
        assertThat(clusterAssignmentCache.getConsumerSubscriptions(consumer1)).containsOnly(subscription1, subscription2);
        assertThat(clusterAssignmentCache.getConsumerSubscriptions(consumer2)).containsOnly(subscription1);
        assertThat(clusterAssignmentCache.getSubscriptionConsumers().get(subscription1)).containsOnly(consumer1, consumer2);
        assertThat(clusterAssignmentCache.getSubscriptionConsumers().get(subscription2)).containsOnly(consumer1);
        assertThat(clusterAssignmentCache.getSubscriptionConsumers().get(subscription3)).isNull();
        assertThat(clusterAssignmentCache.isAssignedTo(consumer1, subscription1)).isTrue();
        assertThat(clusterAssignmentCache.isAssignedTo(consumer1, subscription3)).isFalse();
    }

    @Test
    public void shouldApplyChangesToAssignments() {
        // given
        SubscriptionAssignmentView initView = new SubscriptionAssignmentView(Collections.emptyMap());
        SubscriptionAssignmentView targetView = new SubscriptionAssignmentView(ImmutableMap.of(
                subscription1, ImmutableSet.of(
                        new SubscriptionAssignment(consumer1, subscription1),
                        new SubscriptionAssignment(consumer2, subscription1)
                ),
                subscription2, ImmutableSet.of(
                        new SubscriptionAssignment(consumer1, subscription2)
                )
        ));

        // when
        WorkDistributionChanges changes = registry.updateAssignments(initView, targetView);

        // then
        assertThat(changes.getCreatedAssignmentsCount()).isEqualTo(3);
        assertThat(changes.getDeletedAssignmentsCount()).isZero();

        // when
        clusterAssignmentCache.refresh();

        // then
        assertThat(clusterAssignmentCache.getAssignedConsumers()).containsOnly(consumer1, consumer2);
        assertThat(clusterAssignmentCache.getConsumerSubscriptions(consumer1)).containsOnly(subscription1, subscription2);
        assertThat(clusterAssignmentCache.getConsumerSubscriptions(consumer2)).containsOnly(subscription1);
        assertThat(clusterAssignmentCache.getSubscriptionConsumers().get(subscription1)).containsOnly(consumer1, consumer2);

        // when
        SubscriptionAssignmentView targetView2 = new SubscriptionAssignmentView(ImmutableMap.of(
                subscription1, ImmutableSet.of(
                        new SubscriptionAssignment(consumer2, subscription1)
                ),
                subscription2, ImmutableSet.of(
                        new SubscriptionAssignment(consumer1, subscription2)
                )
        ));
        changes = registry.updateAssignments(clusterAssignmentCache.createSnapshot(), targetView2);

        // then
        assertThat(changes.getCreatedAssignmentsCount()).isEqualTo(0);
        assertThat(changes.getDeletedAssignmentsCount()).isEqualTo(1);

        // when
        clusterAssignmentCache.refresh();

        // then
        assertThat(clusterAssignmentCache.getAssignedConsumers()).containsOnly(consumer1, consumer2);
        assertThat(clusterAssignmentCache.getConsumerSubscriptions(consumer1)).containsOnly(subscription2);
        assertThat(clusterAssignmentCache.getConsumerSubscriptions(consumer2)).containsOnly(subscription1);
    }

    @Test
    public void shouldCleanStaleNodesFromRegistryOnRefresh() {
        // given
        SubscriptionAssignmentView initView = new SubscriptionAssignmentView(Collections.emptyMap());
        SubscriptionAssignmentView targetView = new SubscriptionAssignmentView(ImmutableMap.of(
                subscription1, ImmutableSet.of(
                        new SubscriptionAssignment(consumer1, subscription1),
                        new SubscriptionAssignment(consumer2, subscription1)
                ),
                subscription2, ImmutableSet.of(
                        new SubscriptionAssignment(consumer1, subscription2)
                )
        ));

        // when
        registry.updateAssignments(initView, targetView);

        // and
        clusterAssignmentCache.refresh();

        // then
        assertThat(clusterAssignmentCache.getAssignedConsumers()).containsOnly(consumer1, consumer2);

        // when
        when(consumerNodesRegistry.listConsumerNodes()).thenReturn(Collections.singletonList(consumer2));

        // and
        clusterAssignmentCache.refresh();

        // then
        assertThat(clusterAssignmentCache.getAssignedConsumers()).containsOnly(consumer2);
    }

    @Test
    public void shouldCleanRegistryFromStaleAssignments() {
        // given
        SubscriptionAssignmentView initView = new SubscriptionAssignmentView(Collections.emptyMap());
        SubscriptionAssignmentView targetView = new SubscriptionAssignmentView(ImmutableMap.of(
                subscription1, ImmutableSet.of(
                        new SubscriptionAssignment(consumer1, subscription1),
                        new SubscriptionAssignment(consumer2, subscription1)
                ),
                subscription2, ImmutableSet.of(
                        new SubscriptionAssignment(consumer1, subscription2)
                )
        ));

        // when
        registry.updateAssignments(initView, targetView);

        // then
        wait.untilZookeeperPathIsCreated(registryPaths.consumerWorkloadPath(consumer1));
        wait.untilZookeeperPathIsCreated(registryPaths.consumerWorkloadPath(consumer2));

        // when
        clusterAssignmentCache.refresh();

        // then
        assertThat(clusterAssignmentCache.createSnapshot().getAssignmentsCountForConsumerNode(consumer1)).isEqualTo(2);

        // when subscription2 is removed
        SubscriptionAssignmentView afterRebalanceView = new SubscriptionAssignmentView(ImmutableMap.of(
                subscription1, ImmutableSet.of(
                        new SubscriptionAssignment(consumer1, subscription1),
                        new SubscriptionAssignment(consumer2, subscription1)
                )
        ));
        // and
        registry.updateAssignments(clusterAssignmentCache.createSnapshot(), afterRebalanceView);

        // and
        clusterAssignmentCache.refresh();

        // then
        assertThat(clusterAssignmentCache.createSnapshot().getAssignmentsCountForConsumerNode(consumer1)).isEqualTo(1);
    }
}
