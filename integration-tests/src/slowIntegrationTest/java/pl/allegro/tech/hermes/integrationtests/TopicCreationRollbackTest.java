package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.lifecycle.Startable;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.env.BrokerOperations;
import pl.allegro.tech.hermes.integrationtests.setup.HermesManagementTestApp;
import pl.allegro.tech.hermes.test.helper.client.integration.HermesTestClient;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider.DEFAULT_DC_NAME;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class TopicCreationRollbackTest {

    private static HermesManagementTestApp management;

    private static final ZookeeperContainer hermesZookeeper = new ZookeeperContainer("HermesZookeeper");

    private static final KafkaContainerCluster kafka1 = new KafkaContainerCluster(1);

    private static final KafkaContainerCluster kafka2 = new KafkaContainerCluster(1);

    private static final ConfluentSchemaRegistryContainer schemaRegistry = new ConfluentSchemaRegistryContainer()
            .withKafkaCluster(kafka1);
    private static HermesTestClient hermesApi;

    private static BrokerOperations brokerOperations1;

    @BeforeAll
    public static void setup() {
        Stream.of(hermesZookeeper, kafka1, kafka2)
                .parallel()
                .forEach(Startable::start);
        schemaRegistry.start();
        management = new HermesManagementTestApp(
                Map.of(DEFAULT_DC_NAME, hermesZookeeper),
                Map.of(DEFAULT_DC_NAME, kafka1, "dc2", kafka2),
                schemaRegistry
        );
        management.start();
        hermesApi = new HermesTestClient(management.getPort(), management.getPort(), management.getPort());
        brokerOperations1 = new BrokerOperations(kafka1.getBootstrapServersForExternalClients(), "itTest");
    }

    @AfterAll
    public static void clean() {
        management.stop();
        Stream.of(hermesZookeeper, kafka1, kafka2)
                .parallel()
                .forEach(Startable::stop);
        schemaRegistry.stop();
    }

    @Test
    public void topicCreationRollbackShouldNotDeleteTopicOnBroker() {
        // given
        String groupName = "topicCreationRollbackShouldNotDeleteTopicOnBroker";
        String topicName = "topic";
        String qualifiedTopicName = groupName + "." + topicName;
        hermesApi.createGroup(Group.from(groupName));

        brokerOperations1.createTopic(qualifiedTopicName);
        waitAtMost(Duration.ofMinutes(1)).untilAsserted(() -> assertThat(brokerOperations1.topicExists(qualifiedTopicName)).isTrue());

        // when
        hermesApi.createTopic((topicWithSchema(topic(groupName, topicName).build())));

        // then
        assertThat(brokerOperations1.topicExists(qualifiedTopicName)).isTrue();
    }
}
