package pl.allegro.tech.hermes.integrationtests;

import com.google.pubsub.v1.ReceivedMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesConsumersTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.HermesFrontendExtension;
import pl.allegro.tech.hermes.integrationtests.setup.HermesManagementExtension;
import pl.allegro.tech.hermes.integrationtests.setup.InfrastructureExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestGooglePubSubSubscriber;
import pl.allegro.tech.hermes.test.helper.containers.GooglePubSubContainer;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.io.IOException;
import java.util.List;

import static pl.allegro.tech.hermes.integrationtests.assertions.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class GooglePubSubConsumingTest {

    @Order(0)
    @RegisterExtension
    public static InfrastructureExtension infra = new InfrastructureExtension();

    @Order(1)
    @RegisterExtension
    public static HermesManagementExtension management = new HermesManagementExtension(infra);

    @Order(2)
    @RegisterExtension
    public static HermesFrontendExtension frontend = new HermesFrontendExtension(infra);

    public static final GooglePubSubContainer googlePubSubContainer = new GooglePubSubContainer();

    public static HermesConsumersTestApp consumer;

    @BeforeAll
    public static void startPubSub() {
        googlePubSubContainer.start();
        consumer = new HermesConsumersTestApp(infra.hermesZookeeper(), infra.kafka(), infra.schemaRegistry())
                .googlePubSubEndpoint(googlePubSubContainer.getEmulatorEndpoint());
        consumer.start();
    }

    @AfterAll
    public static void stopPubSub() {
        consumer.stop();
        googlePubSubContainer.stop();
    }

    @Test
    public void shouldDeliverMessageToGooglePubSub() throws IOException {
        // given
        TestGooglePubSubSubscriber subscriber = new TestGooglePubSubSubscriber(googlePubSubContainer);
        Topic topic = management.initHelper().createTopic(topicWithRandomName().build());
        management.initHelper().createSubscription(
                subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build()
        );
        TestMessage message = TestMessage.of("hello", "world");

        // when
        frontend.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilAnyMessageReceived();
        List<ReceivedMessage> allReceivedMessages = subscriber.getAllReceivedMessages();
        assertThat(allReceivedMessages).hasSize(1);
        assertThat(allReceivedMessages.get(0).getMessage())
                .hasAttribute("tn")
                .hasAttribute("id")
                .hasAttribute("ts")
                .hasBody(message.body());

        // cleanup
        subscriber.stop();
    }
}
