package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.consumer.SerialConsumer;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerFactory;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.monitor.ConsumersRuntimeMonitor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.SelectiveSupervisorController;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

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

    private static ConsumerTestRuntimeEnvironment runtime;

    @BeforeClass
    public static void setupAlways() {
        runtime = new ConsumerTestRuntimeEnvironment(ZookeeperBaseTest::newClient);
        runtime.withOverriddenConfigProperty(Configs.CONSUMER_BACKGROUND_SUPERVISOR_INTERVAL, 1000);
        runtime.withOverriddenConfigProperty(Configs.CONSUMER_WORKLOAD_MONITOR_SCAN_INTERVAL, 1);
    }

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

        ConsumersSupervisor supervisor = runtime.consumersSupervisor(consumerFactory);
        SelectiveSupervisorController node = runtime.spawnConsumer("consumer", supervisor);

        runtime.awaitUntilAssignmentExists(runtime.createSubscription(), node);

        // when
        ConsumersRuntimeMonitor monitor = runtime.monitor("consumer", supervisor, node);
        monitor.start();

        // then
        await().atMost(FIVE_SECONDS).until(
                () -> verify(consumerFactory, times(2)).createConsumer(any()));

        shutdown(supervisor);
        shutdown(node);
        shutdown(monitor);

    }

    @Test
    public void shouldCreateConsumerForExistingAssignment() throws InterruptedException {
        // given
        SubscriptionName subscription = runtime.createSubscription();
        runtime.createAssignment(subscription, "consumer");

        // when
        ConsumersSupervisor supervisor = mock(ConsumersSupervisor.class);
        runtime.spawnConsumer("consumer", supervisor);

        // then
        runtime.verifyConsumerWouldBeCreated(supervisor, runtime.getSubscription(subscription));

        shutdown(supervisor);
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
