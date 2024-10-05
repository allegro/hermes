package pl.allegro.tech.hermes.integrationtests;

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class PublishingWithFailoverTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @RegisterExtension
  public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

  @Test
  public void shouldReturn202IfKafkaFailedToRespondButMessageCanBeBufferedInMemory() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    TestMessage message = TestMessage.of("hello", "world");

    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint()).build());
    // we must send first message to a working kafka because producer need to fetch metadata
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());
    subscriber.waitUntilReceived(message.body());

    // when
    hermes.cutOffConnectionsBetweenBrokersAndClients();
    WebTestClient.ResponseSpec response =
        hermes.api().publish(topic.getQualifiedName(), message.body());
    hermes.restoreConnectionsBetweenBrokersAndClients();

    // then
    response.expectStatus().isAccepted();
    subscriber.waitUntilReceived(message.body());
  }
}
