package pl.allegro.tech.hermes.consumers.supervisor.workload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.config.KafkaProperties;
import pl.allegro.tech.hermes.consumers.config.WorkloadProperties;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

public class WorkloadRegistryTest extends ZookeeperBaseTest {

  private static final SubscriptionName subscription1 =
      SubscriptionName.fromString("pl.allegro.tech.hermes$testSubscription");
  private static final SubscriptionName subscription2 =
      SubscriptionName.fromString("pl.allegro.tech.hermes$testSubscription2");
  private static final SubscriptionName subscription3 =
      SubscriptionName.fromString("pl.allegro.tech.hermes$testSubscription3");

  private static final String consumer1 = "consumer1";
  private static final String consumer2 = "consumer2";
  private static final String clusterName = new KafkaProperties().getClusterName();

  private static final ZookeeperPaths zookeeperPaths = new ZookeeperPaths("/hermes");

  private static final WorkloadRegistryPaths registryPaths =
      new WorkloadRegistryPaths(zookeeperPaths, clusterName);

  private static final SubscriptionIds subscriptionIds =
      new TestSubscriptionIds(
          ImmutableList.of(
              SubscriptionId.from(subscription1, 1L),
              SubscriptionId.from(subscription2, 2L),
              SubscriptionId.from(subscription2, 3L)));

  private static final ConsumerAssignmentRegistry registry =
      new ConsumerAssignmentRegistry(
          zookeeperClient,
          new WorkloadProperties().getRegistryBinaryEncoderAssignmentsBufferSizeBytes(),
          clusterName,
          zookeeperPaths,
          subscriptionIds);

  private static final KafkaProperties kafkaProperties = new KafkaProperties();

  private static final String cluster = kafkaProperties.getClusterName();

  private static final ConsumerAssignmentCache assignmentCacheOfConsumer1 =
      new ConsumerAssignmentCache(
          zookeeperClient, consumer1, cluster, zookeeperPaths, subscriptionIds);

  private static final ConsumerAssignmentCache assignmentCacheOfConsumer2 =
      new ConsumerAssignmentCache(
          zookeeperClient, consumer2, cluster, zookeeperPaths, subscriptionIds);

  private static final ConsumerNodesRegistry consumerNodesRegistry =
      mock(ConsumerNodesRegistry.class);

  private static final ClusterAssignmentCache clusterAssignmentCache =
      new ClusterAssignmentCache(
          zookeeperClient, cluster, zookeeperPaths, subscriptionIds, consumerNodesRegistry);

  @BeforeClass
  public static void setUp() throws Exception {
    assignmentCacheOfConsumer1.start();
    assignmentCacheOfConsumer2.start();
  }

  @Before
  public void beforeEach() {
    when(consumerNodesRegistry.listConsumerNodes())
        .thenReturn(ImmutableList.of(consumer1, consumer2));
  }

  @AfterClass
  public static void cleanup() throws Exception {
    assignmentCacheOfConsumer1.stop();
    assignmentCacheOfConsumer2.stop();
  }

  @Test
  public void shouldSaveAssignmentsAndNotifyConsumers() {
    // when
    registry.updateAssignments(consumer1, Set.of(subscription1, subscription2));
    registry.updateAssignments(consumer2, Set.of(subscription1));

    wait.untilZookeeperPathIsCreated(registryPaths.consumerWorkloadPath(consumer1));
    wait.untilZookeeperPathIsCreated(registryPaths.consumerWorkloadPath(consumer2));

    // then
    assertThat(assignmentCacheOfConsumer1.isAssignedTo(subscription1)).isTrue();
    assertThat(assignmentCacheOfConsumer1.isAssignedTo(subscription2)).isTrue();
    assertThat(assignmentCacheOfConsumer1.isAssignedTo(subscription3)).isFalse();

    assertThat(assignmentCacheOfConsumer2.isAssignedTo(subscription1)).isTrue();
    assertThat(assignmentCacheOfConsumer2.isAssignedTo(subscription2)).isFalse();
  }

  @Test
  public void shouldSaveAssignmentsAndReadThroughClusterAssignmentCache() {
    // when
    registry.updateAssignments(consumer1, Set.of(subscription1, subscription2));
    registry.updateAssignments(consumer2, Set.of(subscription1));

    // and
    clusterAssignmentCache.refresh();

    // then
    assertThat(clusterAssignmentCache.getAssignedConsumers()).containsOnly(consumer1, consumer2);
    assertThat(clusterAssignmentCache.getConsumerSubscriptions(consumer1))
        .containsOnly(subscription1, subscription2);
    assertThat(clusterAssignmentCache.getConsumerSubscriptions(consumer2))
        .containsOnly(subscription1);
    assertThat(clusterAssignmentCache.getSubscriptionConsumers().get(subscription1))
        .containsOnly(consumer1, consumer2);
    assertThat(clusterAssignmentCache.getSubscriptionConsumers().get(subscription2))
        .containsOnly(consumer1);
    assertThat(clusterAssignmentCache.getSubscriptionConsumers().get(subscription3)).isNull();
    assertThat(clusterAssignmentCache.isAssignedTo(consumer1, subscription1)).isTrue();
    assertThat(clusterAssignmentCache.isAssignedTo(consumer1, subscription3)).isFalse();
  }

  @Test
  public void shouldApplyChangesToAssignments() {
    // when
    registry.updateAssignments(consumer1, Set.of(subscription1, subscription2));
    registry.updateAssignments(consumer2, Set.of(subscription1));

    // and
    clusterAssignmentCache.refresh();

    // then
    assertThat(clusterAssignmentCache.getAssignedConsumers()).containsOnly(consumer1, consumer2);
    assertThat(clusterAssignmentCache.getConsumerSubscriptions(consumer1))
        .containsOnly(subscription1, subscription2);
    assertThat(clusterAssignmentCache.getConsumerSubscriptions(consumer2))
        .containsOnly(subscription1);
    assertThat(clusterAssignmentCache.getSubscriptionConsumers().get(subscription1))
        .containsOnly(consumer1, consumer2);

    // when
    registry.updateAssignments(consumer1, Set.of(subscription2));
    registry.updateAssignments(consumer2, Set.of(subscription1));

    // and
    clusterAssignmentCache.refresh();

    // then
    assertThat(clusterAssignmentCache.getAssignedConsumers()).containsOnly(consumer1, consumer2);
    assertThat(clusterAssignmentCache.getConsumerSubscriptions(consumer1))
        .containsOnly(subscription2);
    assertThat(clusterAssignmentCache.getConsumerSubscriptions(consumer2))
        .containsOnly(subscription1);
  }

  @Test
  public void shouldCleanStaleNodesFromRegistryOnRefresh() {
    // when
    registry.updateAssignments(consumer1, Set.of(subscription1, subscription2));
    registry.updateAssignments(consumer2, Set.of(subscription1));

    // and
    clusterAssignmentCache.refresh();

    // then
    assertThat(clusterAssignmentCache.getAssignedConsumers()).containsOnly(consumer1, consumer2);

    // when
    when(consumerNodesRegistry.listConsumerNodes())
        .thenReturn(Collections.singletonList(consumer2));

    // and
    clusterAssignmentCache.refresh();

    // then
    assertThat(clusterAssignmentCache.getAssignedConsumers()).containsOnly(consumer2);
  }

  @Test
  public void shouldCleanRegistryFromStaleAssignments() {
    // when
    registry.updateAssignments(consumer1, Set.of(subscription1, subscription2));
    registry.updateAssignments(consumer2, Set.of(subscription1));

    // then
    wait.untilZookeeperPathIsCreated(registryPaths.consumerWorkloadPath(consumer1));
    wait.untilZookeeperPathIsCreated(registryPaths.consumerWorkloadPath(consumer2));

    // when
    clusterAssignmentCache.refresh();

    // then
    assertThat(
            clusterAssignmentCache.createSnapshot().getAssignmentsCountForConsumerNode(consumer1))
        .isEqualTo(2);

    // when subscription2 is removed
    registry.updateAssignments(consumer1, Set.of(subscription1));
    registry.updateAssignments(consumer2, Set.of(subscription1));

    // and
    clusterAssignmentCache.refresh();

    // then
    assertThat(
            clusterAssignmentCache.createSnapshot().getAssignmentsCountForConsumerNode(consumer1))
        .isEqualTo(1);
  }
}
