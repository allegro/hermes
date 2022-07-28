package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.consumer.SerialConsumer;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerFactory;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.monitor.ConsumersRuntimeMonitor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.SelectiveSupervisorController;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.time.Duration;
import java.util.List;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.FIVE_SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class SelectiveSupervisorControllersIntegrationTest extends ZookeeperBaseTest {

    private static final ConsumerTestRuntimeEnvironment runtime = new ConsumerTestRuntimeEnvironment(ZookeeperBaseTest::newClient);

    @Before
    public void setup() throws Exception {
        runtime.killAll();
        deleteData("/hermes");
        createPath("/hermes/groups");
    }

    @Test
    public void shouldRegisterConsumerInActiveNodesRegistryOnStartup() throws Exception {
        // when
        SelectiveSupervisorController controller = runtime.spawnConsumer();

        // then
        runtime.waitForRegistration(controller.consumerId());

        shutdown(controller);
    }

    @Test
    public void shouldElectOnlyOneLeaderFromRegisteredConsumers() throws InterruptedException {
        // when
        List<SelectiveSupervisorController> supervisors = runtime.spawnConsumers(3);

        // then
        assertThat(supervisors.stream().filter(SelectiveSupervisorController::isLeader).count()).isEqualTo(1);

        shutdown(supervisors);
    }

    @Test
    public void shouldElectNewLeaderAfterShutdown() {
        // given
        List<SelectiveSupervisorController> supervisors = runtime.spawnConsumers(3);
        SelectiveSupervisorController leader = runtime.findLeader(supervisors);

        // when
        runtime.kill(leader);

        // then
        await().atMost(adjust(FIVE_SECONDS)).until(() -> runtime.findLeader(supervisors) != leader);
        await().atMost(adjust(FIVE_SECONDS)).until(() -> !leader.isLeader());
    }

    @Test
    public void shouldAssignConsumerToSubscription() throws InterruptedException {
        // given
        SelectiveSupervisorController node = runtime.spawnConsumer();

        // when
        SubscriptionName subscription = runtime.createSubscription();

        // then
        runtime.awaitUntilAssignmentExists(subscription, node);

        shutdown(node);
    }

    @Test
    public void shouldAssignSubscriptionToMultipleConsumers() throws InterruptedException {
        // given
        List<SelectiveSupervisorController> nodes = runtime.spawnConsumers(2);

        // when
        SubscriptionName subscription = runtime.createSubscription();

        // then
        nodes.forEach(node -> runtime.awaitUntilAssignmentExists(subscription, node));

        shutdown(nodes);
    }

    @Test
    public void shouldAssignConsumerToMultipleSubscriptions() throws InterruptedException {
        // given
        SelectiveSupervisorController node = runtime.spawnConsumer();

        // when
        List<SubscriptionName> subscriptions = runtime.createSubscription(2);

        // then
        subscriptions.forEach(subscription -> runtime.awaitUntilAssignmentExists(subscription, node));

        shutdown(node);
    }

    @Test
    public void shouldRecreateMissingConsumer() throws InterruptedException {
        // given
        ConsumerFactory consumerFactory = mock(ConsumerFactory.class);

        when(consumerFactory.createConsumer(any(Subscription.class)))
                .thenThrow(
                        new InternalProcessingException("failed to create consumer"))
                .thenReturn(
                        mock(SerialConsumer.class));

        String consumerId = "consumer";


        ConsumersSupervisor supervisor = runtime.consumersSupervisor(consumerFactory);
        SelectiveSupervisorController node = runtime.spawnConsumer(consumerId, supervisor);

        runtime.awaitUntilAssignmentExists(runtime.createSubscription(), node);

        // when
        ConsumersRuntimeMonitor monitor = runtime.monitor(consumerId, supervisor, node, Duration.ofSeconds(1));
        monitor.start();

        // then
        await().atMost(FIVE_SECONDS).until(
                () -> verify(consumerFactory, times(2)).createConsumer(any()));

        shutdown(supervisor);
        shutdown(node);
        shutdown(monitor);
    }

    private void shutdown(SelectiveSupervisorController controller) throws InterruptedException {
        controller.shutdown();
    }

    private void shutdown(List<SelectiveSupervisorController> controllers) throws InterruptedException {
        for (SelectiveSupervisorController s: controllers) {
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
