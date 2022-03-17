package pl.allegro.tech.hermes.integration;

import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.test.HermesAssertions;
import pl.allegro.tech.hermes.test.helper.endpoint.GooglePubSubEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.core.Response;
import java.io.IOException;

import static javax.ws.rs.core.Response.Status.CREATED;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class PubSubPublishingTest extends IntegrationTest {

    private static GooglePubSubEndpoint googlePubSubEndpoint;

    @BeforeClass
    public static void startUp() throws IOException {
        googlePubSubEndpoint = new GooglePubSubEndpoint(googlePubSubEmulator);
        googlePubSubEndpoint.createTopic(TopicName.format(GOOGLE_PUBSUB_PROJECT_ID, GOOGLE_PUBSUB_TOPIC_ID));
        googlePubSubEndpoint.createSubscription(ProjectSubscriptionName.format(GOOGLE_PUBSUB_PROJECT_ID, GOOGLE_PUBSUB_SUBSCRIPTION_ID),
                TopicName.format(GOOGLE_PUBSUB_PROJECT_ID, GOOGLE_PUBSUB_TOPIC_ID));
    }

    @AfterClass
    public static void cleanup() {
        googlePubSubEndpoint.stop();
    }

    @Test
    public void shouldConsumeMessagesOnMultipleSubscriptions() throws IOException {
        // given
        Topic topic = operations.buildTopic(randomTopic("publishAndConsumeGroup", "topic").build());
        operations.createSubscription(topic, "subscription", GOOGLE_PUBSUB_ENDPOINT_URL);
        TestMessage message = TestMessage.of("hello", "world");

        // when
        Response response = publisher.publish(topic.getQualifiedName(), message.body());

        // then
        assertThat(response).hasStatus(CREATED);
        assertThat(googlePubSubEndpoint.messageReceived(ProjectSubscriptionName.format(GOOGLE_PUBSUB_PROJECT_ID, GOOGLE_PUBSUB_SUBSCRIPTION_ID)))
                .hasAttribute("tn")
                .hasAttribute("id")
                .hasAttribute("ts")
                .hasBody(message.body());
    }
}
