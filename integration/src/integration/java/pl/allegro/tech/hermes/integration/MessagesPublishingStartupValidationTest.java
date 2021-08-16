package pl.allegro.tech.hermes.integration;

import com.google.common.io.Files;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.server.PublishingStartupValidationException;
import pl.allegro.tech.hermes.integration.env.CustomKafkaStarter;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;
import pl.allegro.tech.hermes.test.helper.endpoint.JerseyClientFactory;
import pl.allegro.tech.hermes.test.helper.environment.KafkaStarter;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.ws.rs.client.WebTarget;

import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class MessagesPublishingStartupValidationTest extends IntegrationTest {

    private static final String BROKER_MESSAGES_PUBLISHING_VALIDATION_GROUP = "someRandomGroup";
    private static final String BROKER_MESSAGES_PUBLISHING_VALIDATION_TOPIC = "someRandomTopic";
    private static final int KAFKA_PORT = 9096;

    private KafkaStarter kafka;

    @BeforeMethod
    public void setupEnvironment() throws Exception {
        kafka = setupKafka();
    }

    @AfterMethod
    public void cleanEnvironment() throws Exception {
        kafka.stop();
    }

    @Test
    public void shouldIndicateServiceIsHealthyBasedOnAbilityToPublishMessagesToKafka() throws Exception {
        // given
        int frontendPort = Ports.nextAvailable();
        String frontendUrl = "http://localhost:" + frontendPort + "/";

        // when
        Throwable exception = catchThrowable(() -> setupFrontend(frontendPort));

        // then
        assertThat(exception).isInstanceOf(IllegalStateException.class);
        assertThat(exception.getMessage()).isEqualTo("Missing topic to validate publishing messages on startup");

        Topic topic = operations.buildTopic(TopicBuilder.topic(BROKER_MESSAGES_PUBLISHING_VALIDATION_GROUP, BROKER_MESSAGES_PUBLISHING_VALIDATION_TOPIC).build());
        assertThat(topic).isNotNull();

        // when
        kafka.stop();
        exception = catchThrowable(() -> setupFrontend(frontendPort));

        // then
        assertThat(exception).isInstanceOf(PublishingStartupValidationException.class);
        assertThat(exception.getMessage()).isEqualTo("Error while validating publishing messages, last result: failed:100, success:0");

        // when
        kafka.start();
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
        frontend.overrideProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_ENABLED, true);
        frontend.overrideProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_TIMEOUT_MS, 5000L);
        frontend.overrideProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_TOPIC_NAME, String.format("%s.%s", BROKER_MESSAGES_PUBLISHING_VALIDATION_GROUP, BROKER_MESSAGES_PUBLISHING_VALIDATION_TOPIC));
        frontend.overrideProperty(Configs.KAFKA_BROKER_LIST, "localhost:" + KAFKA_PORT);
        frontend.start();
        return frontend;
    }

    private CustomKafkaStarter setupKafka() throws Exception {
        CustomKafkaStarter kafka = new CustomKafkaStarter(KAFKA_PORT, ZOOKEEPER_CONNECT_STRING + "/validation");
        kafka.start();
        return kafka;
    }
}
