package pl.allegro.tech.hermes.integration;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;

public class UndeliveredLogTest extends IntegrationTest {

    private static final String INVALID_ENDPOINT_URL = "http://localhost:60000";

    @Test
    public void shouldLogUndeliveredMessage() {
        // given
        Topic topic = operations.buildTopic("logUndelivered", "topic");
        Subscription subscription = subscription().withName("subscription")
                .withEndpoint(EndpointAddress.of(INVALID_ENDPOINT_URL))
                .withTrackingEnabled(true)
                .withSubscriptionPolicy(subscriptionPolicy().withRate(1).withMessageTtl(0).build())
                .withSupportTeam("team")
                .build();

        operations.createSubscription(topic, subscription);

        // when
        publisher.publish("logUndelivered.topic", TestMessage.simple().body());
        wait.untilMessageDiscarded();
        Response response = management.subscription().getLatestUndeliveredMessage("logUndelivered.topic", "subscription");

        // then
        wait.until(() -> assertThat(response.getStatusInfo().getFamily()).isEqualTo(SUCCESSFUL));
    }

}
