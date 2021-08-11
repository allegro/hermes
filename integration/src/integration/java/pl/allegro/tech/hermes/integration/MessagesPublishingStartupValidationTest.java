package pl.allegro.tech.hermes.integration;

import com.google.common.io.Files;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.server.PublishingStartupValidationException;
import pl.allegro.tech.hermes.integration.env.CustomKafkaStarter;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.test.helper.endpoint.JerseyClientFactory;
import pl.allegro.tech.hermes.test.helper.environment.KafkaStarter;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.ws.rs.client.WebTarget;

import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class MessagesPublishingStartupValidationTest extends IntegrationTest {

    private static final int KAFKA_PORT = 9096;
    private static final String KAFKA_URL = "localhost:" + KAFKA_PORT;

    private static final String KAFKA_MESSAGES_PUBLISHING_VALIDATION_GROUP = "someRandomGroup";
    private static final String KAFKA_MESSAGES_PUBLISHING_VALIDATION_TOPIC = "someRandomTopic";

    private KafkaStarter kafkaStarter;

    @BeforeClass
    public void setupEnvironment() throws Exception {
        kafkaStarter = setupKafka();
    }

    @AfterClass
    public void cleanEnvironment() throws Exception {
        kafkaStarter.stop();
    }

    @Test
    public void shouldIndicateServiceIsHealthyBasedOnAbilityToPublishMessagesToKafka() throws Exception {
        // given
        int frontendPort = Ports.nextAvailable();
        String frontendUrl = "http://localhost:" + frontendPort + "/";

        operations.createGroup(KAFKA_MESSAGES_PUBLISHING_VALIDATION_GROUP);
        operations.createTopic(KAFKA_MESSAGES_PUBLISHING_VALIDATION_GROUP, KAFKA_MESSAGES_PUBLISHING_VALIDATION_TOPIC);

        // when
        kafkaStarter.stop();
        Throwable exception = catchThrowable(() -> setupFrontend(frontendPort));

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
        frontend.overrideProperty(Configs.FRONTEND_PORT, port);
        frontend.overrideProperty(Configs.METRICS_ZOOKEEPER_REPORTER, false);
        frontend.overrideProperty(Configs.MESSAGES_LOCAL_STORAGE_DIRECTORY, Files.createTempDir().getAbsolutePath());
        frontend.overrideProperty(Configs.ZOOKEEPER_CONNECT_STRING, ZOOKEEPER_CONNECT_STRING);
        frontend.overrideProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_ENABLED, true);
        frontend.overrideProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_TIMEOUT_MS, 5000L);
        frontend.overrideProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_TOPIC_NAME, String.format("%s.%s", KAFKA_MESSAGES_PUBLISHING_VALIDATION_GROUP, KAFKA_MESSAGES_PUBLISHING_VALIDATION_TOPIC));
        frontend.overrideProperty(Configs.KAFKA_BROKER_LIST, KAFKA_URL);
        frontend.start();
        return frontend;
    }

    private CustomKafkaStarter setupKafka() throws Exception {
        CustomKafkaStarter kafkaStarter = new CustomKafkaStarter(KAFKA_PORT, ZOOKEEPER_CONNECT_STRING + "/unhealthyKafka");
        kafkaStarter.start();
        return kafkaStarter;
    }
}
