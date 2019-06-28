package pl.allegro.tech.hermes.integration;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.core.Response;

public class UndeliveredLogTest extends IntegrationTest {

    private static final String INVALID_ENDPOINT_URL = "http://localhost:60000";

    @Test
    public void shouldLogUndeliveredMessage() {
        // given
        Topic topic = operations.buildTopic("logUndelivered", "topic");
        Subscription subscription = subscription(topic, "subscription")
                .withEndpoint(EndpointAddress.of(INVALID_ENDPOINT_URL))
                .withSubscriptionPolicy(subscriptionPolicy().withRate(1).withMessageTtl(0).build())
                .build();

        operations.createSubscription(topic, subscription);

        // when
        publisher.publish("logUndelivered.topic", TestMessage.simple().body());

        // then
        wait.until(() -> {
            Response response = management.subscription().getLatestUndeliveredMessage("logUndelivered.topic", "subscription");
            assertThat(response.getStatusInfo().getFamily()).isEqualTo(SUCCESSFUL);
        });
    }

}
