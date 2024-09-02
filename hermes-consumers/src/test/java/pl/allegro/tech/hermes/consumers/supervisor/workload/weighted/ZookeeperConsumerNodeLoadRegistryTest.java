package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.SubscriptionName.fromString;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.supervisor.workload.TestSubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.test.helper.concurrent.ManuallyTriggeredScheduledExecutorService;
import pl.allegro.tech.hermes.test.helper.concurrent.TestExecutorServiceFactory;
import pl.allegro.tech.hermes.test.helper.metrics.TestMetricsFacadeFactory;
import pl.allegro.tech.hermes.test.helper.time.ModifiableClock;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

public class ZookeeperConsumerNodeLoadRegistryTest extends ZookeeperBaseTest {

  private final SubscriptionName firstSubscription =
      fromString("pl.allegro.tech.hermes$testSubscription");
  private final SubscriptionName secondSubscription =
      fromString("pl.allegro.tech.hermes$testSubscription2");
  private final SubscriptionIds subscriptionIds =
      new TestSubscriptionIds(
          List.of(
              SubscriptionId.from(firstSubscription, 1L),
              SubscriptionId.from(secondSubscription, 2L)));
  private final String currentConsumerId = "consumer-id";
  private final ManuallyTriggeredScheduledExecutorService scheduledExecutorService =
      new ManuallyTriggeredScheduledExecutorService();
  private final ModifiableClock clock = new ModifiableClock();
  private final ZookeeperConsumerNodeLoadRegistry registry =
      new ZookeeperConsumerNodeLoadRegistry(
          zookeeperClient,
          subscriptionIds,
          new ZookeeperPaths("/hermes"),
          currentConsumerId,
          "kafka-cluster",
          Duration.ofMillis(50),
          new TestExecutorServiceFactory(scheduledExecutorService),
          clock,
          TestMetricsFacadeFactory.create(),
          100_000);

  @Before
  public void setUp() {
    registry.start();
  }

  @After
  public void cleanup() throws Exception {
    registry.stop();
    deleteAllNodes();
  }

  @Test
  public void shouldPeriodicallyReportConsumerLoad() {
    // given
    scheduledExecutorService.triggerScheduledTasks();

    // and
    SubscriptionLoadRecorder firstSubscriptionReporter = registry.register(firstSubscription);
    firstSubscriptionReporter.initialize();
    firstSubscriptionReporter.recordSingleOperation();
    firstSubscriptionReporter.recordSingleOperation();

    // and
    SubscriptionLoadRecorder secondSubscriptionReporter = registry.register(secondSubscription);
    secondSubscriptionReporter.initialize();

    // and
    clock.advanceMinutes(1);

    // when
    scheduledExecutorService.triggerScheduledTasks();

    // then
    ConsumerNodeLoad consumerNodeLoad = registry.get(currentConsumerId);
    Map<SubscriptionName, SubscriptionLoad> loadPerSubscription =
        consumerNodeLoad.getLoadPerSubscription();
    assertThat(loadPerSubscription).hasSize(2);
    assertThat(loadPerSubscription.get(firstSubscription).getOperationsPerSecond())
        .isEqualTo(2d / 60);
    assertThat(loadPerSubscription.get(secondSubscription).getOperationsPerSecond()).isEqualTo(0d);
  }

  @Test
  public void shouldNotReportLoadOfRemovedSubscription() {
    // given
    scheduledExecutorService.triggerScheduledTasks();

    // and
    SubscriptionLoadRecorder firstSubscriptionReporter = registry.register(firstSubscription);
    firstSubscriptionReporter.initialize();

    // when
    scheduledExecutorService.triggerScheduledTasks();

    // then
    assertThat(registry.get(currentConsumerId).getLoadPerSubscription()).hasSize(1);

    // when
    firstSubscriptionReporter.shutdown();
    scheduledExecutorService.triggerScheduledTasks();

    // then
    assertThat(registry.get(currentConsumerId).getLoadPerSubscription()).hasSize(0);
  }
}
