package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.PatchData.patchData;

public class BasicAuthSubscribingTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldAuthorizeUsingBasicAuthWhenSubscriptionHasCredentials() {
        // given
        Topic topic = operations.buildTopic("basicAuthGroup", "topic");
        operations.createSubscription(topic, "subscription", "http://user:password@localhost:" + remoteService.getServicePort() + "/");

        TestMessage message = TestMessage.of("hello", "world");
        remoteService.expectMessages(message.body());

        // when
        publisher.publish(topic.getQualifiedName(), message.body());

        // then
        remoteService.waitUntilReceived();
        assertThat(remoteService.receivedMessageWithHeader("Authorization", "Basic dXNlcjpwYXNzd29yZA==")).isTrue();
    }

    @Test
    public void shouldUpdateSubscriptionUsernameAndPassword() {
        // given
        Topic topic = operations.buildTopic("basicAuthEditGroup", "topic");
        operations.createSubscription(topic, "subscription", "http://user:password@localhost:1234");

        // when
        operations.updateSubscription("basicAuthEditGroup", "topic", "subscription",
                patchData().set("endpoint", "http://newuser:newpassword@localhost:1234").build());

        // then
        Subscription subscription = management.subscription().get("basicAuthEditGroup.topic", "subscription");
        assertThat(subscription.getEndpoint().getUsername()).isEqualTo("newuser");
    }
}
