package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.helper.RemoteJmsEndpoint;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class JmsConsumingTest {

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    private static final String JMS_TOPIC_NAME = "hermes";

    private RemoteJmsEndpoint jmsEndpoint;

    @BeforeEach
    public void initialize() {
        this.jmsEndpoint = new RemoteJmsEndpoint(JMS_TOPIC_NAME);
    }

    @Test
    public void shouldConsumeMessageOnJMSEndpoint() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(), "subscription1", subscriber.getEndpoint()).build());
        TestMessage message = TestMessage.of("hello", "world");

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilReceived(message.body());
    }
}
