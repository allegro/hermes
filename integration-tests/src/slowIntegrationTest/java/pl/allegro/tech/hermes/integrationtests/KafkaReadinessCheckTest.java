package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.lifecycle.Startable;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesFrontendTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.HermesManagementTestApp;
import pl.allegro.tech.hermes.test.helper.client.integration.FrontendTestClient;
import pl.allegro.tech.hermes.test.helper.client.integration.HermesInitHelper;
import pl.allegro.tech.hermes.test.helper.containers.BrokerId;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;
import pl.allegro.tech.hermes.test.helper.environment.HermesTestApp;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static pl.allegro.tech.hermes.api.Topic.Ack.ALL;
import static pl.allegro.tech.hermes.integrationtests.assertions.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class KafkaReadinessCheckTest {

    private static final ZookeeperContainer hermesZookeeper = new ZookeeperContainer("HermesZookeeper");
    private static final KafkaContainerCluster kafka = new KafkaContainerCluster(3);
    private static final ConfluentSchemaRegistryContainer schemaRegistry = new ConfluentSchemaRegistryContainer()
            .withKafkaCluster(kafka);
    private static Topic topic;

    @BeforeAll
    public static void setup() {
        Stream.of(hermesZookeeper, kafka).parallel().forEach(Startable::start);
        schemaRegistry.start();
        HermesTestApp management = new HermesManagementTestApp(hermesZookeeper, kafka, schemaRegistry)
                .start();

        HermesInitHelper hermesInitHelper = new HermesInitHelper(management.getPort());
        topic = hermesInitHelper.createTopic(
                topicWithRandomName()
                        .withAck(ALL)
                        .build()
        );
        management.stop();
    }

    @AfterAll
    public static void clean() {
        Stream.of(hermesZookeeper, kafka, schemaRegistry).parallel().forEach(Startable::stop);
    }

    @Test
    public void shouldNotBeReadyUntilKafkaClusterIsUp() {
        // given
        kafka.cutOffConnectionsBetweenBrokersAndClients();

        // when
        HermesTestApp hermesFrontend = new HermesFrontendTestApp(hermesZookeeper, kafka, schemaRegistry)
                .metadataMaxAgeInSeconds(1)
                .readinessCheckIntervalInSeconds(1)
                .kafkaCheckEnabled()
                .start();

        // then
        getStatusReady(hermesFrontend).expectStatus().isEqualTo(SERVICE_UNAVAILABLE);
        getStatusHealth(hermesFrontend).expectStatus().is2xxSuccessful();

        // when
        kafka.restoreConnectionsBetweenBrokersAndClients();

        // then
        await().atMost(5, SECONDS)
                .untilAsserted(() -> getStatusReady(hermesFrontend).expectStatus().is2xxSuccessful());
        getStatusHealth(hermesFrontend).expectStatus().is2xxSuccessful();

        // cleanup
        hermesFrontend.stop();
    }

    @Test
    public void shouldRespectKafkaCheckEnabledFlag() {
        // given
        kafka.cutOffConnectionsBetweenBrokersAndClients();

        // when
        HermesTestApp hermesFrontend = new HermesFrontendTestApp(hermesZookeeper, kafka, schemaRegistry)
                .metadataMaxAgeInSeconds(1)
                .readinessCheckIntervalInSeconds(1)
                .kafkaCheckDisabled()
                .start();

        // then
        getStatusReady(hermesFrontend).expectStatus().is2xxSuccessful();
        getStatusHealth(hermesFrontend).expectStatus().is2xxSuccessful();

        // cleanup
        kafka.restoreConnectionsBetweenBrokersAndClients();
        hermesFrontend.stop();
    }

    @Test
    public void shouldNotBeReadyUntilThereAreNoUnderReplicatedPartitions() throws Exception {
        // given
        List<BrokerId> brokers = kafka.getAllBrokers();
        List<BrokerId> brokersToStop = brokers.subList(kafka.getMinInSyncReplicas() - 1, brokers.size());
        kafka.stop(brokersToStop);
        assertThat(kafka.countUnderReplicatedPartitions() > 0).isTrue();

        // when
        HermesTestApp hermesFrontend = new HermesFrontendTestApp(hermesZookeeper, kafka, schemaRegistry)
                .metadataMaxAgeInSeconds(1)
                .readinessCheckIntervalInSeconds(1)
                .kafkaCheckEnabled()
                .start();

        // then
        getStatusReady(hermesFrontend).expectStatus().isEqualTo(SERVICE_UNAVAILABLE);
        getStatusHealth(hermesFrontend).expectStatus().is2xxSuccessful();

        // when
        kafka.start(selectOne(brokersToStop));

        // then
        await().atMost(5, SECONDS)
                .untilAsserted(() -> getStatusReady(hermesFrontend).expectStatus().is2xxSuccessful());
        getStatusHealth(hermesFrontend).expectStatus().is2xxSuccessful();

        // cleanup
        kafka.startAllStoppedBrokers();
        hermesFrontend.stop();
    }

    @Test
    public void shouldNotBeReadyUntilThereAreNoOfflinePartitions() throws Exception {
        // given: stop one of the brokers
        List<BrokerId> brokers = kafka.getAllBrokers();
        List<BrokerId> brokersToStop = selectOne(brokers);
        kafka.stop(brokersToStop);

        // and: send a message to remove stopped broker from in-sync replicas
        publishSampleMessage(topic);

        // and: stop all in-sync replicas
        List<BrokerId> isr = selectAllOtherThan(brokers, brokersToStop);
        kafka.stop(isr);

        // and: start the broker that is not in in-sync replicas
        kafka.start(brokersToStop);

        // and: check if there is at least one offline partition
        assertThat(kafka.countOfflinePartitions() > 0).isTrue();

        // when
        HermesTestApp hermesFrontend = new HermesFrontendTestApp(hermesZookeeper, kafka, schemaRegistry)
                .metadataMaxAgeInSeconds(1)
                .readinessCheckIntervalInSeconds(1)
                .kafkaCheckEnabled()
                .start();

        // then
        getStatusReady(hermesFrontend).expectStatus().isEqualTo(SERVICE_UNAVAILABLE);
        getStatusHealth(hermesFrontend).expectStatus().is2xxSuccessful();

        // when
        kafka.start(isr);

        // then
        await().atMost(5, SECONDS)
                .untilAsserted(() -> getStatusReady(hermesFrontend).expectStatus().is2xxSuccessful());
        getStatusHealth(hermesFrontend).expectStatus().is2xxSuccessful();

        // cleanup
        hermesFrontend.stop();
    }

    private static List<BrokerId> selectOne(List<BrokerId> brokerIds) {
        return brokerIds.subList(0, 1);
    }

    private static List<BrokerId> selectAllOtherThan(List<BrokerId> brokerIds, List<BrokerId> toExclude) {
        return brokerIds.stream().filter(b -> !toExclude.contains(b)).collect(Collectors.toList());
    }

    private void publishSampleMessage(Topic topic) {
        HermesTestApp hermesFrontend = new HermesFrontendTestApp(hermesZookeeper, kafka, schemaRegistry)
                .start();
        FrontendTestClient client = new FrontendTestClient(hermesFrontend.getPort());
        client.publishUntilSuccess(topic.getQualifiedName(), "message");
        hermesFrontend.stop();
    }

    private WebTestClient.ResponseSpec getStatusHealth(HermesTestApp hermesFrontend) {
        FrontendTestClient client = new FrontendTestClient(hermesFrontend.getPort());
        return client.getStatusHealth();
    }

    private WebTestClient.ResponseSpec getStatusReady(HermesTestApp hermesFrontend) {
        FrontendTestClient client = new FrontendTestClient(hermesFrontend.getPort());
        return client.getStatusReady();
    }
}
