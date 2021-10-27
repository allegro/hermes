package pl.allegro.tech.hermes.integration;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.setup.HermesFrontendInstance;
import pl.allegro.tech.hermes.integration.setup.HermesManagementInstance;
import pl.allegro.tech.hermes.test.helper.containers.BrokerId;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static pl.allegro.tech.hermes.api.Topic.Ack.ALL;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.management.infrastructure.dc.DefaultDatacenterNameProvider.DEFAULT_DC_NAME;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class ReadinessCheckTest extends BaseIntegrationTest {
    private static final String DC1 = DEFAULT_DC_NAME;
    private static final String DC2 = "dc2";

    private HermesManagementInstance hermesManagement;
    private Topic topic;

    @BeforeClass
    public void setup() throws Exception {
        hermesManagement = HermesManagementInstance.starter()
                .addKafkaCluster(DC1, kafkaClusterOne.getBootstrapServers())
                .addKafkaCluster(DC2, kafkaClusterTwo.getBootstrapServers())
                .addZookeeperCluster(DC1, hermesZookeeperOne.getConnectionString())
                .addZookeeperCluster(DC2, hermesZookeeperTwo.getConnectionString())
                .replicationFactor(kafkaClusterOne.getAllBrokers().size())
                .uncleanLeaderElectionEnabled(false)
                .start();
        topic = hermesManagement.operations().buildTopic(
                randomTopic("someRandomGroup", "someRandomTopic")
                        .withAck(ALL)
                        .build()
        );
    }

    @AfterClass
    public void cleanup() {
        hermesManagement.stop();
    }

    @Test
    public void shouldNotBeReadyUntilKafkaClusterIsUp() {
        // given
        kafkaClusterOne.stopAllBrokers();

        // when
        HermesFrontendInstance hermesFrontend = HermesFrontendInstance.starter()
                .metadataMaxAgeInSeconds(1)
                .readinessCheckIntervalInSeconds(1)
                .zookeeperConnectionString(hermesZookeeperOne.getConnectionString())
                .kafkaConnectionString(kafkaClusterOne.getBootstrapServers())
                .start();

        // then
        assertThat(hermesFrontend.isReady()).isFalse();
        assertThat(hermesFrontend.isHealthy()).isTrue();

        // when
        kafkaClusterOne.startAllBrokers();

        // then
        await().atMost(5, SECONDS).until(() ->
                assertThat(hermesFrontend.isReady()).isTrue()
        );
        assertThat(hermesFrontend.isHealthy()).isTrue();

        // cleanup
        hermesFrontend.stop();
    }

    @Test
    public void shouldNotBeReadyUntilThereAreNoUnderReplicatedPartitions() throws Exception {
        // given
        List<BrokerId> brokers = kafkaClusterOne.getAllBrokers();
        List<BrokerId> brokersToStop = brokers.subList(kafkaClusterOne.getMinInSyncReplicas() - 1, brokers.size());
        kafkaClusterOne.stop(brokersToStop);
        assertThat(kafkaClusterOne.countUnderReplicatedPartitions() > 0).isTrue();

        // when
        HermesFrontendInstance hermesFrontend = HermesFrontendInstance.starter()
                .metadataMaxAgeInSeconds(1)
                .readinessCheckIntervalInSeconds(1)
                .zookeeperConnectionString(hermesZookeeperOne.getConnectionString())
                .kafkaConnectionString(kafkaClusterOne.getBootstrapServers())
                .start();

        // then
        assertThat(hermesFrontend.isReady()).isFalse();
        assertThat(hermesFrontend.isHealthy()).isTrue();

        // when
        kafkaClusterOne.start(selectOne(brokersToStop));

        // then
        await().atMost(5, SECONDS).until(() ->
                assertThat(hermesFrontend.isReady()).isTrue()
        );
        assertThat(hermesFrontend.isHealthy()).isTrue();

        // cleanup
        hermesFrontend.stop();
    }

    @Test
    public void shouldNotBeReadyUntilThereAreNoOfflinePartitions() throws Exception {
        // given: stop one of the brokers
        List<BrokerId> brokers = kafkaClusterOne.getAllBrokers();
        List<BrokerId> brokersToStop = selectOne(brokers);
        kafkaClusterOne.stop(brokersToStop);

        // and: send a message to remove stopped broker from in-sync replicas
        publishSampleMessage(topic);

        // and: stop all in-sync replicas
        List<BrokerId> isr = selectAllOtherThan(brokers, brokersToStop);
        kafkaClusterOne.stop(isr);

        // and: start the broker that is not in in-sync replicas
        kafkaClusterOne.start(brokersToStop);

        // and: check if there is at least one offline partition
        assertThat(kafkaClusterOne.countOfflinePartitions() > 0).isTrue();

        // when
        HermesFrontendInstance hermesFrontend = HermesFrontendInstance.starter()
                .metadataMaxAgeInSeconds(1)
                .readinessCheckIntervalInSeconds(1)
                .zookeeperConnectionString(hermesZookeeperOne.getConnectionString())
                .kafkaConnectionString(kafkaClusterOne.getBootstrapServers())
                .start();

        // then
        assertThat(hermesFrontend.isReady()).isFalse();
        assertThat(hermesFrontend.isHealthy()).isTrue();

        // when
        kafkaClusterOne.start(isr);

        // then
        await().atMost(5, SECONDS).until(() ->
                assertThat(hermesFrontend.isReady()).isTrue()
        );
        assertThat(hermesFrontend.isHealthy()).isTrue();

        // cleanup
        hermesFrontend.stop();
    }

    @Test
    public void shouldRespectReadinessStatusSetByAdmin() {
        // given
        HermesFrontendInstance hermesFrontendDc1 = HermesFrontendInstance.starter()
                .readinessCheckIntervalInSeconds(1)
                .zookeeperConnectionString(hermesZookeeperOne.getConnectionString())
                .kafkaConnectionString(kafkaClusterOne.getBootstrapServers())
                .start();
        HermesFrontendInstance hermesFrontendDc2 = HermesFrontendInstance.starter()
                .readinessCheckIntervalInSeconds(1)
                .zookeeperConnectionString(hermesZookeeperTwo.getConnectionString())
                .kafkaConnectionString(kafkaClusterTwo.getBootstrapServers())
                .start();

        // when
        hermesManagement.operations().setReadiness(DC1, false);

        // then
        await().atMost(5, SECONDS).until(() -> {
            assertThat(hermesFrontendDc1.isReady()).isFalse();
            assertThat(hermesFrontendDc2.isReady()).isTrue();
        });

        // when
        hermesManagement.operations().setReadiness(DC1, true);

        // then
        await().atMost(5, SECONDS).until(() -> {
            assertThat(hermesFrontendDc1.isReady()).isTrue();
            assertThat(hermesFrontendDc2.isReady()).isTrue();
        });

        // cleanup
        hermesFrontendDc1.stop();
        hermesFrontendDc2.stop();
    }

    private static List<BrokerId> selectOne(List<BrokerId> brokerIds) {
        return brokerIds.subList(0, 1);
    }

    private static List<BrokerId> selectAllOtherThan(List<BrokerId> brokerIds, List<BrokerId> toExclude) {
        return brokerIds.stream().filter(b -> !toExclude.contains(b)).collect(Collectors.toList());
    }

    private void publishSampleMessage(Topic topic) {
        HermesFrontendInstance hermesFrontend = HermesFrontendInstance.starter()
                .zookeeperConnectionString(hermesZookeeperOne.getConnectionString())
                .kafkaConnectionString(kafkaClusterOne.getBootstrapServers())
                .start();
        Response publish = hermesFrontend.operations().publish(topic.getQualifiedName(), "message");
        assertThat(publish).hasStatus(CREATED);
        hermesFrontend.stop();
    }
}
