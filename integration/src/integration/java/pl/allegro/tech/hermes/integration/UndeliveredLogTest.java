package pl.allegro.tech.hermes.integration;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;

public class UndeliveredLogTest extends IntegrationTest {

    private static final String INVALID_ENDPOINT_URL = "http://localhost:60000";
    private static final TestMessage MESSAGE = TestMessage.of("hello", "world");

    @Test
    public void shouldLogUndeliveredMessage() {
        // given
        operations.buildTopic("logUndelivered", "topic");
        Subscription subscription = subscription().withName("subscription")
                .withEndpoint(EndpointAddress.of(INVALID_ENDPOINT_URL))
                .withTrackingEnabled(true)
                .withSubscriptionPolicy(subscriptionPolicy().withRate(1).withMessageTtl(3).build())
                .build();

        operations.createSubscription("logUndelivered", "topic", subscription);

        // when
        publisher.publish("logUndelivered.topic", MESSAGE.body());
        wait.untilMessageDiscarded();
        Response response = management.subscription().getLatestUndeliveredMessage("logUndelivered.topic", "subscription");

        // then
        assertThat(response.getStatusInfo().getFamily()).isEqualTo(SUCCESSFUL);

    }

}
