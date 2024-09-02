package pl.allegro.tech.hermes.consumers.supervisor.workload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

import java.time.Duration;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.consumer.SerialConsumer;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerFactory;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.monitor.ConsumersRuntimeMonitor;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

public class WorkloadSupervisorIntegrationTest extends ZookeeperBaseTest {

  private static final ConsumerTestRuntimeEnvironment runtime =
      new ConsumerTestRuntimeEnvironment(ZookeeperBaseTest::newClient);

  @Before
  public void setup() throws Exception {
    runtime.cleanState();
    deleteData("/hermes");
    createPath("/hermes/groups");
  }

  @Test
  public void shouldRegisterConsumerInActiveNodesRegistryOnStartup() throws Exception {
    // when
    WorkloadSupervisor controller = runtime.spawnConsumer();

    // then
    runtime.waitForRegistration(controller.consumerId());

    shutdown(controller);
  }

  @Test
  public void shouldElectOnlyOneLeaderFromRegisteredConsumers() throws Exception {
    // when
    List<WorkloadSupervisor> supervisors = runtime.spawnConsumers(3);

    // then
    assertThat(supervisors.stream().filter(WorkloadSupervisor::isLeader).count()).isEqualTo(1);

    shutdown(supervisors);
  }

  @Test
  public void shouldElectNewLeaderAfterShutdown() {
    // given
    List<WorkloadSupervisor> supervisors = runtime.spawnConsumers(3);
    WorkloadSupervisor leader = runtime.findLeader(supervisors);

    // when
    runtime.kill(leader);

    // then
    await()
        .atMost(adjust(Duration.ofSeconds(5)))
        .until(() -> runtime.findLeader(supervisors) != leader);
    await().atMost(adjust(Duration.ofSeconds(5))).until(() -> !leader.isLeader());
  }

  @Test
  public void shouldAssignConsumerToSubscription() throws Exception {
    // given
    WorkloadSupervisor node = runtime.spawnConsumer();

    // when
    SubscriptionName subscription = runtime.createSubscription();

    // then
    runtime.awaitUntilAssignmentExists(subscription, node);

    shutdown(node);
  }

  @Test
  public void shouldAssignSubscriptionToMultipleConsumers() throws Exception {
    // given
    List<WorkloadSupervisor> nodes = runtime.spawnConsumers(2);

    // when
    SubscriptionName subscription = runtime.createSubscription();

    // then
    nodes.forEach(node -> runtime.awaitUntilAssignmentExists(subscription, node));

    shutdown(nodes);
  }

  @Test
  public void shouldAssignConsumerToMultipleSubscriptions() throws Exception {
    // given
    WorkloadSupervisor node = runtime.spawnConsumer();

    // when
    List<SubscriptionName> subscriptions = runtime.createSubscription(2);

    // then
    subscriptions.forEach(subscription -> runtime.awaitUntilAssignmentExists(subscription, node));

    shutdown(node);
  }

  @Test
  public void shouldRecreateMissingConsumer() throws Exception {
    // given
    ConsumerFactory consumerFactory = mock(ConsumerFactory.class);

    when(consumerFactory.createConsumer(any(Subscription.class)))
        .thenThrow(new InternalProcessingException("failed to create consumer"))
        .thenReturn(mock(SerialConsumer.class));

    String consumerId = "consumer";

    ConsumersSupervisor supervisor = runtime.consumersSupervisor(consumerFactory);
    WorkloadSupervisor node = runtime.spawnConsumer(consumerId, supervisor);

    runtime.awaitUntilAssignmentExists(runtime.createSubscription(), node);

    // when
    ConsumersRuntimeMonitor monitor =
        runtime.monitor(consumerId, supervisor, node, Duration.ofSeconds(1));
    monitor.start();

    // then
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> verify(consumerFactory, times(2)).createConsumer(any()));

    shutdown(supervisor);
    shutdown(node);
    shutdown(monitor);
  }

  private void shutdown(WorkloadSupervisor workloadSupervisor) throws Exception {
    workloadSupervisor.shutdown();
  }

  private void shutdown(List<WorkloadSupervisor> workloadSupervisors) throws Exception {
    for (WorkloadSupervisor s : workloadSupervisors) {
      s.shutdown();
    }
  }

  private void shutdown(ConsumersRuntimeMonitor monitor) throws InterruptedException {
    monitor.shutdown();
  }

  private void shutdown(ConsumersSupervisor supervisor) throws InterruptedException {
    supervisor.shutdown();
  }
}
