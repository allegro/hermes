package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.client.HermesTestClient;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import static pl.allegro.tech.hermes.integrationtests.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class PublishingAndConsumingTest {

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    @RegisterExtension
    public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

    @Test
    public void shouldPublishAndConsumeMessage() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = hermes.api().createGroupAndTopic(topic("testGroup", "testTopic1").build());
        hermes.api().createSubscription(subscription(topic.getQualifiedName(), "subscription1", subscriber.getEndpoint()).build());
        TestMessage message = TestMessage.of("hello", "world");

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilReceived(message.body());
    }

    @Test
    public void shouldConsumeMessagesOnMultipleSubscriptions() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        Topic topic = hermes.api().createGroupAndTopic(topic("pl.allegro.testTopic2").build());
        TestSubscriber subscriber1 = subscribers.createSubscriber();
        TestSubscriber subscriber2 = subscribers.createSubscriber();
        hermes.api().createSubscription(subscription(topic.getQualifiedName(), "subscription1", subscriber1.getEndpoint()).build());
        hermes.api().createSubscription(subscription(topic.getQualifiedName(), "subscription2", subscriber2.getEndpoint()).build());

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber1.waitUntilReceived(message.body());
        subscriber2.waitUntilReceived(message.body());
    }

    @Test
    public void shouldPassSubscriptionFixedHeaders() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        Topic topic = hermes.api().createGroupAndTopic(topic("pl.allegro.testTopic3").build());
        TestSubscriber subscriber = subscribers.createSubscriber();
        Subscription subscription = SubscriptionBuilder.subscriptionWithRandomName(topic.getName())
                .withEndpoint(subscriber.getEndpoint())
                .withHeader("MY-HEADER", "myHeader123")
                .build();
        hermes.api().createSubscription(subscription);

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request).hasHeaderValue("MY-HEADER", "myHeader123");
            assertThat(request.getHeader("Hermes-Message-Id")).isNotEmpty();
        });
    }
}
