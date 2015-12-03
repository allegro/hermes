package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.SelectiveSupervisorController;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.util.List;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.FIVE_SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class SelectiveSupervisorControllersIntegrationTest extends ZookeeperBaseTest {
    private ConsumerTestRuntimeEnvironment runtime;

    @Before
    public void setup() throws Exception {
        runtime = new ConsumerTestRuntimeEnvironment(ZookeeperBaseTest::otherClient);
    }

    @After
    public void cleanup() throws Exception {
        deleteAllNodes();
    }

    @Test
    public void shouldRegisterConsumerOnStartup() throws Exception {
        // given
        String id = "supervisor1";

        // when
        runtime.node(id).start();

        // then
        runtime.waitForRegistration(id);
    }

    @Test
    public void shouldElectOnlyOneLeaderFromRegisteredConsumers() {
        // given
        List<SelectiveSupervisorController> supervisors = runtime.nodes(3);

        // when
        supervisors.forEach(runtime::startNode);

        // then
        assertThat(supervisors.stream().filter(SelectiveSupervisorController::isLeader).count()).isEqualTo(1);
    }

    @Test
    public void shouldElectNewLeaderAfterShutdown() throws InterruptedException {
        // given
        List<SelectiveSupervisorController> supervisors = runtime.nodes(3);
        supervisors.forEach(runtime::startNode);
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
        SelectiveSupervisorController node = runtime.spawnNode();

        // when
        SubscriptionName subscription = runtime.createSubscription("com.example.topic$test");

        // then
        runtime.awaitUntilAssignmentExists(subscription, node);
    }

    @Test
    public void shouldAssignSubscriptionToMultipleConsumers() {
        // given
        List<SelectiveSupervisorController> nodes = runtime.spawnNodes(2);

        // when
        SubscriptionName subscription = runtime.createSubscription("com.example.topic$test");

        // then
        nodes.forEach(node -> runtime.awaitUntilAssignmentExists(subscription, node));
    }

    @Test
    public void shouldAssignConsumerToMultipleSubscriptions() {
        // given
        SelectiveSupervisorController node = runtime.spawnNode();

        // when
        List<SubscriptionName> subscriptions = runtime.createSubscription(2);

        // then
        subscriptions.forEach(subscription -> runtime.awaitUntilAssignmentExists(subscription, node));
    }
}
