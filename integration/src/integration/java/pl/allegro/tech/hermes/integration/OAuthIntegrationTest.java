package pl.allegro.tech.hermes.integration;

import com.jayway.awaitility.Duration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionOAuthPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;
import pl.allegro.tech.hermes.test.helper.oauth.server.OAuthTestServer;

import java.io.IOException;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.CREATED;
import static pl.allegro.tech.hermes.api.SubscriptionOAuthPolicy.clientCredentialsGrantOAuthPolicy;
import static pl.allegro.tech.hermes.api.SubscriptionOAuthPolicy.passwordGrantOAuthPolicy;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.OAuthProviderBuilder.oAuthProvider;

public class OAuthIntegrationTest extends IntegrationTest {

    private OAuthTestServer oAuthTestServer;

    SubscriptionOAuthPolicy usernamePasswordOAuthPolicy = passwordGrantOAuthPolicy("provider1")
            .withUsername("testUser1")
            .withPassword("password1")
            .build();

    SubscriptionOAuthPolicy clientCredentialsOAuthPolicy = clientCredentialsGrantOAuthPolicy("provider1")
            .build();

    @BeforeClass
    public void initialize() throws IOException {
        oAuthTestServer = new OAuthTestServer();
        oAuthTestServer.start();
        oAuthTestServer.registerClient("client1", "secret1");
        oAuthTestServer.registerResourceOwner("testUser1", "password1");

        OAuthProvider provider = oAuthProvider("provider1")
                .withTokenEndpoint(oAuthTestServer.getTokenEndpoint())
                .withClientId("client1")
                .withClientSecret("secret1")
                .withRequestTimeout(10 * 1000)
                .withTokenRequestInitialDelay(100)
                .withTokenRequestMaxDelay(10000)
                .build();

        operations.createOAuthProvider(provider);
    }

    @AfterClass
    public void tearDown() {
        oAuthTestServer.stop();
    }

    @BeforeMethod
    public void initializeAlways() {
        oAuthTestServer.revokeAllTokens();
        oAuthTestServer.clearResourceAccessCounters();
        oAuthTestServer.clearTokenIssueCounters();
    }

    @Test
    public void shouldSendMessageToUsernamePasswordGrantOAuthSecuredEndpoint() {
        // given
        Topic topic = operations.buildTopic("publishAndConsumeOAuthGroup", "topic1");
        Subscription subscription = SubscriptionBuilder.subscription(topic, "subscription1")
                .withEndpoint(oAuthTestServer.getUsernamePasswordSecuredResourceEndpoint("testUser1"))
                .withOAuthPolicy(usernamePasswordOAuthPolicy).build();
        operations.createSubscription(topic, subscription);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), "hello world");
        assertThat(response).hasStatus(CREATED);

        // then
        wait.awaitAtMost(Duration.TEN_SECONDS).until(() -> oAuthTestServer.getResourceAccessCount("testUser1") == 1);
    }

    @Test
    public void shouldInvalidateRevokedAndRequestNewTokenForUsernamePasswordGrantOAuthSecuredEndpoint() {
        // given
        Topic topic = operations.buildTopic("publishAndConsumeOAuthGroup2", "topic2");
        Subscription subscription = SubscriptionBuilder.subscription(topic, "subscription2")
                .withEndpoint(oAuthTestServer.getUsernamePasswordSecuredResourceEndpoint("testUser1"))
                .withOAuthPolicy(usernamePasswordOAuthPolicy).build();
        operations.createSubscription(topic, subscription);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), "hello world");
        assertThat(response).hasStatus(CREATED);

        // then
        wait.awaitAtMost(Duration.TEN_SECONDS).until(() -> oAuthTestServer.getResourceAccessCount("testUser1") == 1);

        // and when
        oAuthTestServer.revokeAllTokens();
        response = publisher.publish(topic.getQualifiedName(), "hello again");
        assertThat(response).hasStatus(CREATED);

        // then
        wait.awaitAtMost(Duration.TEN_SECONDS).until(() -> oAuthTestServer.getResourceAccessCount("testUser1") == 2);
    }

    @Test
    public void shouldSendMessageToClientCredentialsGrantOAuthSecuredEndpoint() {
        // given
        Topic topic = operations.buildTopic("publishAndConsumeOAuthGroup", "topic3");
        Subscription subscription = SubscriptionBuilder.subscription(topic, "subscription3")
                .withEndpoint(oAuthTestServer.getClientCredentialsSecuredResourceEndpoint("client1"))
                .withOAuthPolicy(clientCredentialsOAuthPolicy).build();
        operations.createSubscription(topic, subscription);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), "hello world");
        assertThat(response).hasStatus(CREATED);

        // then
        wait.awaitAtMost(Duration.TEN_SECONDS).until(() -> oAuthTestServer.getResourceAccessCount("client1") == 1);
    }

    @Test
    public void shouldInvalidateRevokedAndRequestNewTokenForClientCredentialsGrantSecuredEndpoint() {
        // given
        Topic topic = operations.buildTopic("publishAndConsumeOAuthGroup", "topic4");
        Subscription subscription = SubscriptionBuilder.subscription(topic, "subscription4")
                .withEndpoint(oAuthTestServer.getClientCredentialsSecuredResourceEndpoint("client1"))
                .withOAuthPolicy(clientCredentialsOAuthPolicy).build();
        operations.createSubscription(topic, subscription);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), "hello world");
        assertThat(response).hasStatus(CREATED);

        // then
        wait.awaitAtMost(Duration.TEN_SECONDS).until(() -> oAuthTestServer.getResourceAccessCount("client1") == 1);

        // and when
        oAuthTestServer.revokeAllTokens();
        response = publisher.publish(topic.getQualifiedName(), "hello again");
        assertThat(response).hasStatus(CREATED);

        // then
        wait.awaitAtMost(Duration.TEN_SECONDS).until(() -> oAuthTestServer.getResourceAccessCount("client1") == 2);
    }
}
