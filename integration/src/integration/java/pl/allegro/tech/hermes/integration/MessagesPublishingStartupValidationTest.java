package pl.allegro.tech.hermes.integration;

import com.google.common.io.Files;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.server.PublishingStartupValidationException;
import pl.allegro.tech.hermes.integration.env.CustomKafkaStarter;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.integration.env.ManagementStarter;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesAPIOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;
import pl.allegro.tech.hermes.test.helper.endpoint.JerseyClientFactory;
import pl.allegro.tech.hermes.test.helper.environment.KafkaStarter;
import pl.allegro.tech.hermes.test.helper.environment.ZookeeperStarter;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.ws.rs.client.WebTarget;

import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class MessagesPublishingStartupValidationTest extends IntegrationTest {

    private static final String BROKER_MESSAGES_PUBLISHING_VALIDATION_GROUP = "someRandomGroup";
    private static final String BROKER_MESSAGES_PUBLISHING_VALIDATION_TOPIC = "someRandomTopic";
    private static final int ZOOKEEPER_PORT = 14194;
    private static final String ZOOKEEPER_URL = "localhost:" + ZOOKEEPER_PORT;
    private static final int MANAGEMENT_PORT = 18084;
    private static final String MANAGEMENT_URL = "http://localhost:" + MANAGEMENT_PORT + "/";
    private static final int KAFKA_PORT = 9098;
    private static final String KAFKA_URL = "localhost:" + KAFKA_PORT;

    private ZookeeperStarter zookeeper;
    private HermesAPIOperations operations;
    private ManagementStarter hermesManagement;
    private KafkaStarter kafkaStarter;

    @BeforeClass
    public void setupEnvironment() throws Exception {
        zookeeper = setupZookeeper();
        kafkaStarter = setupKafka();
        hermesManagement = setupHermesManagement();
        operations = setupOperations();
    }

    @AfterClass
    public void cleanEnvironment() throws Exception {
        hermesManagement.stop();
        kafkaStarter.stop();
        zookeeper.stop();
    }

    @Test
    public void shouldReturnCorrectReadinessStatusBasedOnKafkaReadiness() throws Exception {
        // given
        int frontendPort = Ports.nextAvailable();
        String frontendUrl = "http://localhost:" + frontendPort + "/";

        // when
        Throwable exception = catchThrowable(() -> setupFrontend(frontendPort));

        // then
        assertThat(exception).isInstanceOf(IllegalStateException.class);
        assertThat(exception.getMessage()).isEqualTo("Missing topic to validate publishing messages on startup");

        // when
        Topic topic = operations.buildTopic(TopicBuilder.topic(BROKER_MESSAGES_PUBLISHING_VALIDATION_GROUP, BROKER_MESSAGES_PUBLISHING_VALIDATION_TOPIC).build());
        assertThat(topic).isNotNull();

        // when
        kafkaStarter.stop();
        exception = catchThrowable(() -> setupFrontend(frontendPort));

        // then
        assertThat(exception).isInstanceOf(PublishingStartupValidationException.class);
        assertThat(exception.getMessage()).isEqualTo("Error while validating publishing messages, last result: failed:100, success:0");

        // when
        kafkaStarter.start();
        FrontendStarter frontend = setupFrontend(frontendPort);
        WebTarget clientHealth = JerseyClientFactory.create().target(frontendUrl).path("status").path("health");

        // then
        assertThat(clientHealth.request().get()).hasStatus(OK);

        // cleanup
        frontend.stop();
    }

    private FrontendStarter setupFrontend(int port) throws Exception {
        FrontendStarter frontend = new FrontendStarter(port, false);
        frontend.overrideProperty(Configs.METRICS_ZOOKEEPER_REPORTER, false);
        frontend.overrideProperty(Configs.MESSAGES_LOCAL_STORAGE_ENABLED, false);
        frontend.overrideProperty(Configs.MESSAGES_LOCAL_STORAGE_DIRECTORY, Files.createTempDir().getAbsolutePath());
        frontend.overrideProperty(Configs.ZOOKEEPER_CONNECT_STRING, ZOOKEEPER_URL);
        frontend.overrideProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_ENABLED, true);
        frontend.overrideProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_TIMEOUT_MS, 5000L);
        frontend.overrideProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_TOPIC_NAME, String.format("%s.%s", BROKER_MESSAGES_PUBLISHING_VALIDATION_GROUP, BROKER_MESSAGES_PUBLISHING_VALIDATION_TOPIC));
        frontend.overrideProperty(Configs.KAFKA_BROKER_LIST, KAFKA_URL);
        frontend.start();
        return frontend;
    }

    private ZookeeperStarter setupZookeeper() throws Exception {
        ZookeeperStarter zookeeperStarter = new ZookeeperStarter(ZOOKEEPER_PORT, ZOOKEEPER_URL, CONFIG_FACTORY.getStringProperty(Configs.ZOOKEEPER_ROOT));
        zookeeperStarter.start();
        return zookeeperStarter;
    }

    private ManagementStarter setupHermesManagement() throws Exception {
        ManagementStarter managementStarter = new ManagementStarter(MANAGEMENT_PORT, "validation");
        managementStarter.start();
        return managementStarter;
    }

    private HermesAPIOperations setupOperations() {
        HermesEndpoints management = new HermesEndpoints(MANAGEMENT_URL, CONSUMER_ENDPOINT_URL);
        return new HermesAPIOperations(management, wait);
    }

    private CustomKafkaStarter setupKafka() throws Exception {
        CustomKafkaStarter kafkaStarter = new CustomKafkaStarter(KAFKA_PORT, ZOOKEEPER_URL + "/validation");
        kafkaStarter.start();
        return kafkaStarter;
    }
}
