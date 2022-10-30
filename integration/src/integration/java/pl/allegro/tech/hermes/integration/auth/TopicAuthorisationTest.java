package pl.allegro.tech.hermes.integration.auth;

import io.undertow.util.StatusCodes;
import org.assertj.core.description.Description;
import org.assertj.core.description.TextDescription;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_AUTHENTICATION_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_AUTHENTICATION_MODE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_SSL_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.KAFKA_BROKER_LIST;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.SCHEMA_REPOSITORY_SERVER_URL;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.ZOOKEEPER_CONNECTION_STRING;
import static pl.allegro.tech.hermes.integration.auth.SingleUserAwareIdentityManager.getHeadersWithAuthentication;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class TopicAuthorisationTest extends IntegrationTest {

    public static final int FRONTEND_PORT = Ports.nextAvailable();
    public static final String FRONTEND_URL = "http://127.0.0.1:" + FRONTEND_PORT;

    private static final String USERNAME = "someUser";
    private static final String PASSWORD = "somePassword123";
    private static final String MESSAGE = TestMessage.of("hello", "world").body();

    private static final String USERNAME2 = "foobar";

    protected HermesPublisher publisher;

    private FrontendStarter frontendStarter;

    @BeforeClass
    public void setup() throws Exception {
        frontendStarter = new FrontendStarter(FRONTEND_PORT);
        frontendStarter.addSpringProfiles("authNonRequired");
        frontendStarter.overrideProperty(FrontendConfigurationProperties.FRONTEND_PORT, FRONTEND_PORT);
        frontendStarter.overrideProperty(FRONTEND_SSL_ENABLED, false);
        frontendStarter.overrideProperty(FRONTEND_AUTHENTICATION_MODE, "pro_active");
        frontendStarter.overrideProperty(FRONTEND_AUTHENTICATION_ENABLED, true);
        frontendStarter.overrideProperty(KAFKA_BROKER_LIST, kafkaClusterOne.getBootstrapServersForExternalClients());
        frontendStarter.overrideProperty(ZOOKEEPER_CONNECTION_STRING, hermesZookeeperOne.getConnectionString());
        frontendStarter.overrideProperty(SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());

        frontendStarter.start();
        publisher = new HermesPublisher(FRONTEND_URL);
        operations.buildTopic("someGroup", "topicWithAuthorization");
    }

    @AfterClass
    public void tearDown() throws Exception {
        frontendStarter.stop();
    }

    @Test
    public void shouldPublishWhenAuthenticated() {
        // given
        List<Topic> topics = Arrays.asList(
                TopicBuilder.topic("disabled.authenticated")
                        .build(),
                TopicBuilder.topic("enabled.authenticated_1Publisher")
                        .withPublisher(USERNAME)
                        .withAuthEnabled()
                        .build(),
                TopicBuilder.topic("enabled.authenticated_2Publishers")
                        .withPublisher(USERNAME)
                        .withPublisher(USERNAME2)
                        .withAuthEnabled()
                        .build(),
                TopicBuilder.topic("required.authenticated_1Publisher")
                        .withPublisher(USERNAME)
                        .withAuthEnabled()
                        .withUnauthenticatedAccessDisabled()
                        .build(),
                TopicBuilder.topic("required.authenticated_2Publishers")
                        .withPublisher(USERNAME)
                        .withPublisher(USERNAME2)
                        .withAuthEnabled()
                        .withUnauthenticatedAccessDisabled()
                        .build()
        );

        topics.forEach(operations::buildTopic);

        Map<String, String> headers = getHeadersWithAuthentication(USERNAME, PASSWORD);

        topics.forEach(topic -> {
            // when
            Response response = publisher.publish(topic.getQualifiedName(), MESSAGE, headers);

            // then
            assertThat(response.getStatusInfo().getFamily()).as(description(topic)).isEqualTo(SUCCESSFUL);
        });
    }

    @Test
    public void shouldPublishAsGuestWhenAuthIsNotRequired() {
        // given
        List<Topic> topics = Arrays.asList(
                TopicBuilder.topic("disabled.guest")
                        .build(),
                TopicBuilder.topic("enabled.guest_0Publishers")
                        .withAuthEnabled()
                        .build(),
                TopicBuilder.topic("enabled.guest_1Publisher")
                        .withPublisher(USERNAME2)
                        .withAuthEnabled()
                        .build()
        );

        topics.forEach(operations::buildTopic);

        topics.forEach(topic -> {
            // when
            Response response = publisher.publish(topic.getQualifiedName(), MESSAGE);

            // then
            assertThat(response.getStatusInfo().getFamily()).as(description(topic)).isEqualTo(SUCCESSFUL);
        });
    }

    @Test
    public void shouldNotPublishAsGuestWhenAuthIsRequired() {
        // given
        List<Topic> topics = Arrays.asList(
                TopicBuilder.topic("required.guest_0Publishers")
                        .withAuthEnabled()
                        .withUnauthenticatedAccessDisabled()
                        .build(),
                TopicBuilder.topic("required.guest_1Publisher")
                        .withPublisher(USERNAME2)
                        .withAuthEnabled()
                        .withUnauthenticatedAccessDisabled()
                        .build()
        );

        topics.forEach(operations::buildTopic);

        topics.forEach(topic -> {
            // when
            Response response = publisher.publish(topic.getQualifiedName(), MESSAGE);

            // then
            assertThat(response.getStatus()).as(description(topic)).isEqualTo(StatusCodes.FORBIDDEN);
        });
    }

    @Test
    public void shouldNotPublishWithoutPermissionWhenAuthenticated() {
        // given
        List<Topic> topics = Arrays.asList(
                TopicBuilder.topic("enabled.authenticated_no_permission_0Publishers")
                        .withAuthEnabled()
                        .build(),
                TopicBuilder.topic("enabled.authenticated_no_permission_1Publisher")
                        .withPublisher(USERNAME2)
                        .withAuthEnabled()
                        .build(),
                TopicBuilder.topic("required.authenticated_no_permission_0Publishers")
                        .withAuthEnabled()
                        .withUnauthenticatedAccessDisabled()
                        .build(),
                TopicBuilder.topic("required.authenticated_no_permission_1Publisher")
                        .withPublisher(USERNAME2)
                        .withAuthEnabled()
                        .withUnauthenticatedAccessDisabled()
                        .build()
        );

        topics.forEach(operations::buildTopic);

        Map<String, String> headers = getHeadersWithAuthentication(USERNAME, PASSWORD);

        topics.forEach(topic -> {
            // when
            Response response = publisher.publish(topic.getQualifiedName(), MESSAGE, headers);

            // then
            assertThat(response.getStatus()).as(description(topic)).isEqualTo(StatusCodes.FORBIDDEN);
        });
    }

    private Description description(Topic topic) {
        return new TextDescription("topic=%s", topic.getQualifiedName());
    }
}
