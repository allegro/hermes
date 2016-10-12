package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.consumer.SerialConsumer;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerFactory;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
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
        String consumerId = runtime.spawnConsumer().watchedConsumerId().get();

        // then
        runtime.waitForRegistration(consumerId);
    }

    @Test
    public void shouldElectOnlyOneLeaderFromRegisteredConsumers() {
        // when
        List<SelectiveSupervisorController> supervisors = runtime.spawnConsumers(3);

        // then
        assertThat(supervisors.stream().filter(SelectiveSupervisorController::isLeader).count()).isEqualTo(1);
    }

    @Test
    public void shouldElectNewLeaderAfterShutdown() throws InterruptedException {
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
    public void shouldAssignConsumerToSubscription() {
        // given
        SelectiveSupervisorController node = runtime.spawnConsumer();

        // when
        SubscriptionName subscription = runtime.createSubscription();

        // then
        runtime.awaitUntilAssignmentExists(subscription, node);
    }

    @Test
    public void shouldAssignSubscriptionToMultipleConsumers() {
        // given
        List<SelectiveSupervisorController> nodes = runtime.spawnConsumers(2);

        // when
        SubscriptionName subscription = runtime.createSubscription();

        // then
        nodes.forEach(node -> runtime.awaitUntilAssignmentExists(subscription, node));
    }

    @Test
    public void shouldAssignConsumerToMultipleSubscriptions() {
        // given
        SelectiveSupervisorController node = runtime.spawnConsumer();

        // when
        List<SubscriptionName> subscriptions = runtime.createSubscription(2);

        // then
        subscriptions.forEach(subscription -> runtime.awaitUntilAssignmentExists(subscription, node));
    }

    @Test
    public void shouldRecreateMissingConsumer() {
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
        runtime.monitor("consumer", supervisor, node).run();

        // then
        verify(consumerFactory, times(2)).createConsumer(any());
    }
}
