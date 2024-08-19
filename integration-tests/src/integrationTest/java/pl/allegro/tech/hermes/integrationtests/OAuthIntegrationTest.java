package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.SubscriptionOAuthPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.oauth.server.OAuthClient;
import pl.allegro.tech.hermes.test.helper.oauth.server.OAuthResourceOwner;
import pl.allegro.tech.hermes.test.helper.oauth.server.OAuthTestServer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.not;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.hc.core5.http.HttpStatus.SC_UNAUTHORIZED;
import static pl.allegro.tech.hermes.api.SubscriptionOAuthPolicy.clientCredentialsGrantOAuthPolicy;
import static pl.allegro.tech.hermes.api.SubscriptionOAuthPolicy.passwordGrantOAuthPolicy;
import static pl.allegro.tech.hermes.test.helper.builder.OAuthProviderBuilder.oAuthProvider;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class OAuthIntegrationTest {

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    @RegisterExtension
    public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

    private static final OAuthTestServer oAuthTestServer = new OAuthTestServer();

    private static final OAuthClient oAuthClient = new OAuthClient("client1", "secret1");

    private static final OAuthResourceOwner resourceOwner = new OAuthResourceOwner("testUser1", "password1");

    private final SubscriptionOAuthPolicy usernamePasswordOAuthPolicy = passwordGrantOAuthPolicy("provider1")
            .withUsername(resourceOwner.username())
            .withPassword(resourceOwner.password())
            .build();

    private final SubscriptionOAuthPolicy clientCredentialsOAuthPolicy = clientCredentialsGrantOAuthPolicy("provider1")
            .build();

    @BeforeAll
    public static void initialize() {
        oAuthTestServer.start();
        hermes.initHelper().createOAuthProvider(
                oAuthProvider("provider1")
                        .withTokenEndpoint(oAuthTestServer.getTokenEndpoint())
                        .withClientId(oAuthClient.clientId())
                        .withClientSecret(oAuthClient.secret())
                        .withRequestTimeout(10 * 1000)
                        .withTokenRequestInitialDelay(100)
                        .withTokenRequestMaxDelay(10000)
                        .build()
        );
    }

    @AfterAll
    public static void tearDown() {
        oAuthTestServer.stop();
    }

    @BeforeEach
    public void initializeAlways() {
        oAuthTestServer.reset();
    }

    @Test
    public void shouldSendMessageToUsernamePasswordGrantOAuthSecuredEndpoint() {
        // given
        String token = oAuthTestServer.stubAccessTokenForPasswordGrant(oAuthClient, resourceOwner);
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        hermes.initHelper().createSubscription(
                subscription(topic, "subscription1")
                        .withEndpoint(subscriber.getEndpoint())
                        .withOAuthPolicy(usernamePasswordOAuthPolicy)
                        .build()
        );

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), "hello world");

        // then
        subscriber.waitUntilMessageWithHeaderReceived("Authorization", "Bearer " + token);
    }

    @Test
    public void shouldRequestNewTokenIfPreviousIsInvalidForUsernamePasswordGrantOAuthSecuredEndpoint() {
        // given
        String invalidToken = oAuthTestServer.stubAccessTokenForPasswordGrant(oAuthClient, resourceOwner);
        TestSubscriber subscriber = subscribers.createSubscriber((service, endpoint) -> {
            service.addStubMapping((post(endpoint))
                    .withHeader("Authorization", equalTo("Bearer " + invalidToken))
                    .willReturn(aResponse().withStatus(SC_UNAUTHORIZED)).build());

            service.addStubMapping((post(endpoint))
                    .withHeader("Authorization", not(equalTo("Bearer " + invalidToken)))
                    .willReturn(aResponse().withStatus(SC_OK)).build());
        });
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        hermes.initHelper().createSubscription(
                subscription(topic, "subscription2")
                        .withEndpoint(subscriber.getEndpoint())
                        .withOAuthPolicy(usernamePasswordOAuthPolicy)
                        .build()
        );

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), "hello world");

        // then
        subscriber.waitUntilMessageWithHeaderReceived("Authorization", "Bearer " + invalidToken);

        // and when
        String validToken = oAuthTestServer.stubAccessTokenForPasswordGrant(oAuthClient, resourceOwner);

        // then
        subscriber.waitUntilMessageWithHeaderReceived("Authorization", "Bearer " + validToken);
    }

    @Test
    public void shouldSendMessageToClientCredentialsGrantOAuthSecuredEndpoint() {
        // given
        String token = oAuthTestServer.stubAccessTokenForClientCredentials(oAuthClient);
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        hermes.initHelper().createSubscription(
                subscription(topic, "subscription3")
                        .withEndpoint(subscriber.getEndpoint())
                        .withOAuthPolicy(clientCredentialsOAuthPolicy)
                        .build()
        );

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), "hello world");

        // then
        subscriber.waitUntilMessageWithHeaderReceived("Authorization", "Bearer " + token);
    }

    @Test
    public void shouldRequestNewTokenIfPreviousIsInvalidForClientCredentialsGrantSecuredEndpoint() {
        // given
        String invalidToken = oAuthTestServer.stubAccessTokenForClientCredentials(oAuthClient);
        TestSubscriber subscriber = subscribers.createSubscriber((service, endpoint) -> {
            service.addStubMapping((post(endpoint))
                    .withHeader("Authorization", equalTo("Bearer " + invalidToken))
                    .willReturn(aResponse().withStatus(SC_UNAUTHORIZED)).build());

            service.addStubMapping((post(endpoint))
                    .withHeader("Authorization", not(equalTo("Bearer " + invalidToken)))
                    .willReturn(aResponse().withStatus(SC_OK)).build());
        });
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        hermes.initHelper().createSubscription(
                subscription(topic, "subscription4")
                        .withEndpoint(subscriber.getEndpoint())
                        .withOAuthPolicy(clientCredentialsOAuthPolicy)
                        .build()
        );

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), "hello world");

        // then
        subscriber.waitUntilMessageWithHeaderReceived("Authorization", "Bearer " + invalidToken);

        // and when
        String validToken = oAuthTestServer.stubAccessTokenForClientCredentials(oAuthClient);

        // then
        subscriber.waitUntilMessageWithHeaderReceived("Authorization", "Bearer " + validToken);
    }
}
