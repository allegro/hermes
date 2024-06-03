package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.time.Duration;

import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class UndeliveredLogTest {

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    private static final String INVALID_ENDPOINT_URL = "http://localhost:60000";

    @Test
    public void shouldLogUndeliveredMessage() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        Subscription subscription = hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName(), INVALID_ENDPOINT_URL).build());

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), TestMessage.simple().body());

        // then
        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() ->
                hermes.api().getLatestUndeliveredMessage(topic.getQualifiedName(), subscription.getName()).expectStatus().is2xxSuccessful()
        );
    }
}