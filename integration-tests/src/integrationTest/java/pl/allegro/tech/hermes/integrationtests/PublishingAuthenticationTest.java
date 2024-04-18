package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.utils.Headers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.AUTH_PASSWORD;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.AUTH_USERNAME;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_AUTHENTICATION_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_AUTHENTICATION_MODE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_SSL_ENABLED;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class PublishingAuthenticationTest {

    private static final String USERNAME = "someUser";
    private static final String PASSWORD = "somePassword123";
    private static final String MESSAGE = TestMessage.of("hello", "world").body();

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension()
            .withFrontendProfile("authRequired")
            .withFrontendProperty(AUTH_USERNAME, USERNAME)
            .withFrontendProperty(AUTH_PASSWORD, PASSWORD)
            .withFrontendProperty(FRONTEND_SSL_ENABLED, false)
            .withFrontendProperty(FRONTEND_AUTHENTICATION_MODE, "pro_active")
            .withFrontendProperty(FRONTEND_AUTHENTICATION_ENABLED, true);

    @Test
    public void shouldAuthenticateUsingBasicAuth() {
        //given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

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

    @Test
    public void shouldNotAuthenticateUserWithInvalidCredentials() {
        //given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            // when
            WebTestClient.ResponseSpec response = hermes.api().publish(
                    topic.getQualifiedName(),
                    MESSAGE,
                    createAuthorizationHeader(USERNAME, "someInvalidPassword")
            );

            // then
            response.expectStatus().isUnauthorized();
        });
    }

    @Test
    public void shouldNotAuthenticateUserWithoutCredentials() {
        //given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            // when
            WebTestClient.ResponseSpec response = hermes.api().publish(topic.getQualifiedName(), MESSAGE);

            // then
            response.expectStatus().isUnauthorized();
        });
    }

    private static HttpHeaders createAuthorizationHeader(String username, String password) {
        String credentials = username + ":" + password;
        Map<String, String> headers = Map.of(
                "Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8))
        );
        return Headers.createHeaders(headers);
    }
}
