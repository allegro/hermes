package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.integration.helper.RemoteJmsEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class JmsConsumingTest extends IntegrationTest {

    private RemoteJmsEndpoint jmsEndpoint;

    private final static String JMS_TOPIC_NAME = "hermes";

    @BeforeMethod
    public void initializeAlways() {
        this.jmsEndpoint = new RemoteJmsEndpoint(JMS_TOPIC_NAME);
    }

    @Test
    public void shouldConsumeMessageOnJMSEndpoint() {
        // given
        operations.buildSubscription("publishJmsGroup", "topic", "subscription", jmsEndpointAddress(JMS_TOPIC_NAME));
        jmsEndpoint.expectMessages(TestMessage.of("hello", "world"));

        // when
        publisher.publish("publishJmsGroup.topic", TestMessage.of("hello", "world").body());

        // then
        jmsEndpoint.waitUntilReceived();
    }

    private String jmsEndpointAddress(String topicName) {
        return "jms://guest:guest@localhost:5445/" + topicName;
    }

}
