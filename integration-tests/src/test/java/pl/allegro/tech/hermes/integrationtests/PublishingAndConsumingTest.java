package pl.allegro.tech.hermes.integrationtests;

import jakarta.ws.rs.core.Response;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.util.stream.Stream;

import static jakarta.ws.rs.core.Response.Status.CREATED;
import static pl.allegro.tech.hermes.integrationtests.HermesAssertions.assertThat;

public class PublishingAndConsumingTest {

    public static final KafkaContainerCluster kafkaCluster = new KafkaContainerCluster(1);

    private final HermesManagementOperations operations = new HermesManagementOperations();
    private final TestPublisher publisher = new TestPublisher();
    private final TestSubscribers subscribers = new TestSubscribers();

    @BeforeClass
    public static void setup() {
        Stream.of(kafkaCluster)
                .parallel()
                .forEach(Startable::start);

    }

    @Test
    public void shouldPublishAndConsumeMessage() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = operations.createRandomTopic();
        operations.createRandomSubscription(topic, subscriber.getEndpoint());
        TestMessage message = TestMessage.of("hello", "world");

        // when
        // TODO: consider publisher.publish(topic, message);
        Response response = publisher.publish(topic.getQualifiedName(), message.body());

        // then
        assertThat(response).hasStatus(CREATED);
        subscriber.waitUntilReceived(message.body());
    }

    @Test
    public void shouldConsumeMessagesOnMultipleSubscriptions() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        Topic topic = operations.createRandomTopic();
        TestSubscriber subscriber1 = subscribers.createSubscriber();
        TestSubscriber subscriber2 = subscribers.createSubscriber();
        operations.createRandomSubscription(topic, subscriber1.getEndpoint());
        operations.createRandomSubscription(topic, subscriber2.getEndpoint());

        // when
        publisher.publish(topic.getQualifiedName(), message.body());

        // then
        subscriber1.waitUntilReceived(message.body());
        subscriber2.waitUntilReceived(message.body());
    }

    @Test
    public void shouldPassSubscriptionFixedHeaders() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        Topic topic = operations.createRandomTopic();
        TestSubscriber subscriber = subscribers.createSubscriber();
        Subscription subscription = SubscriptionBuilder.subscriptionWithRandomName(topic.getName())
                .withEndpoint(subscriber.getEndpoint())
                .withHeader("MY-HEADER", "myHeader123")
                .build();
        operations.createSubscription(topic, subscription);

        // when
        publisher.publish(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request).hasHeaderValue("MY-HEADER", "myHeader123");
            assertThat(request.getHeader("Hermes-Message-Id")).isNotEmpty();
        });
    }
}
