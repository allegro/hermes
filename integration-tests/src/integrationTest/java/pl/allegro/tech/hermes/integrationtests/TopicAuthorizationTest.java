package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.utils.Headers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.AUTH_PASSWORD;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.AUTH_USERNAME;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_AUTHENTICATION_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_AUTHENTICATION_MODE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_SSL_ENABLED;

public class TopicAuthorizationTest {

    private static final String USERNAME = "someUser";
    private static final String PASSWORD = "somePassword123";
    private static final String MESSAGE = TestMessage.of("hello", "world").body();
    private static final String USERNAME2 = "foobar";

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension()
            .withFrontendProfile("authNonRequired")
            .withFrontendProperty(AUTH_USERNAME, USERNAME)
            .withFrontendProperty(AUTH_PASSWORD, PASSWORD)
            .withFrontendProperty(FRONTEND_SSL_ENABLED, false)
            .withFrontendProperty(FRONTEND_AUTHENTICATION_MODE, "pro_active")
            .withFrontendProperty(FRONTEND_AUTHENTICATION_ENABLED, true);

    @ParameterizedTest
    @MethodSource("publishWhenAuthenticatedTopics")
    public void shouldPublishWhenAuthenticated(Topic topic) {
        // given
        hermes.initHelper().createTopic(topic);

        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            // when
            WebTestClient.ResponseSpec response = hermes.api().publish(
                    topic.getQualifiedName(),
                    MESSAGE,
                    createAuthorizationHeader(USERNAME, PASSWORD)
            );

            // then
            response.expectStatus().isCreated();
        });
    }

    static Stream<Topic> publishWhenAuthenticatedTopics() {
        return Stream.of(
                TopicBuilder.topicWithRandomName()
                        .build(),
                TopicBuilder.topicWithRandomName()
                        .withPublisher(USERNAME)
                        .withAuthEnabled()
                        .build(),
                TopicBuilder.topicWithRandomName()
                        .withPublisher(USERNAME)
                        .withPublisher(USERNAME2)
                        .withAuthEnabled()
                        .build(),
                TopicBuilder.topicWithRandomName()
                        .withPublisher(USERNAME)
                        .withAuthEnabled()
                        .withUnauthenticatedAccessDisabled()
                        .build(),
                TopicBuilder.topicWithRandomName()
                        .withPublisher(USERNAME)
                        .withPublisher(USERNAME2)
                        .withAuthEnabled()
                        .withUnauthenticatedAccessDisabled()
                        .build()
        );
    }

    @ParameterizedTest
    @MethodSource("publishAsGuestWhenAuthIsNotRequiredTopics")
    public void shouldPublishAsGuestWhenAuthIsNotRequired(Topic topic) {
        // given
        hermes.initHelper().createTopic(topic);

        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            // when
            WebTestClient.ResponseSpec response = hermes.api().publish(topic.getQualifiedName(), MESSAGE);

            // then
            response.expectStatus().isCreated();
        });
    }

    static Stream<Topic> publishAsGuestWhenAuthIsNotRequiredTopics() {
        return Stream.of(
                TopicBuilder.topicWithRandomName()
                        .build(),
                TopicBuilder.topicWithRandomName()
                        .withAuthEnabled()
                        .build(),
                TopicBuilder.topicWithRandomName()
                        .withPublisher(USERNAME2)
                        .withAuthEnabled()
                        .build()
        );
    }

    @ParameterizedTest
    @MethodSource("notPublishAsGuestWhenAuthIsRequiredTopics")
    public void shouldNotPublishAsGuestWhenAuthIsRequired(Topic topic) {
        // given
        hermes.initHelper().createTopic(topic);

        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            // when
            WebTestClient.ResponseSpec response = hermes.api().publish(topic.getQualifiedName(), MESSAGE);

            // then
            response.expectStatus().isForbidden();
        });
    }

    static Stream<Topic> notPublishAsGuestWhenAuthIsRequiredTopics() {
        return Stream.of(
                TopicBuilder.topicWithRandomName()
                        .withAuthEnabled()
                        .withUnauthenticatedAccessDisabled()
                        .build(),
                TopicBuilder.topicWithRandomName()
                        .withPublisher(USERNAME2)
                        .withAuthEnabled()
                        .withUnauthenticatedAccessDisabled()
                        .build()
        );
    }

    @ParameterizedTest
    @MethodSource("notPublishWithoutPermissionWhenAuthenticatedTopics")
    public void shouldNotPublishWithoutPermissionWhenAuthenticated(Topic topic) {
        // given
        hermes.initHelper().createTopic(topic);

        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            // when
            WebTestClient.ResponseSpec response = hermes.api().publish(
                    topic.getQualifiedName(),
                    MESSAGE,
                    createAuthorizationHeader(USERNAME, PASSWORD)
            );

            // then
            response.expectStatus().isForbidden();
        });
    }

    static Stream<Topic> notPublishWithoutPermissionWhenAuthenticatedTopics() {
        return Stream.of(
                TopicBuilder.topicWithRandomName()
                        .withAuthEnabled()
                        .build(),
                TopicBuilder.topicWithRandomName()
                        .withPublisher(USERNAME2)
                        .withAuthEnabled()
                        .build(),
                TopicBuilder.topicWithRandomName()
                        .withAuthEnabled()
                        .withUnauthenticatedAccessDisabled()
                        .build(),
                TopicBuilder.topicWithRandomName()
                        .withPublisher(USERNAME2)
                        .withAuthEnabled()
                        .withUnauthenticatedAccessDisabled()
                        .build()
        );
    }

    private static HttpHeaders createAuthorizationHeader(String username, String password) {
        String credentials = username + ":" + password;
        Map<String, String> headers = Map.of(
                "Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8))
        );
        return Headers.createHeaders(headers);
    }
}
