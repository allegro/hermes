package pl.allegro.tech.hermes.integrationtests;

import static pl.allegro.tech.hermes.integrationtests.assertions.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import com.google.pubsub.v1.ReceivedMessage;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.GooglePubSubExtension;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestGooglePubSubSubscriber;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class GooglePubSubConsumingTest {

  @Order(0)
  @RegisterExtension
  public static final GooglePubSubExtension googlePubSub = new GooglePubSubExtension();

  @Order(1)
  @RegisterExtension
  public static final HermesExtension hermes = new HermesExtension().withGooglePubSub(googlePubSub);

  @Test
  public void shouldDeliverMessageToGooglePubSub() throws IOException {
    // given
    TestGooglePubSubSubscriber subscriber = googlePubSub.subscriber();
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    hermes
        .initHelper()
        .createSubscription(
            subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint())
                .build());
    TestMessage message = TestMessage.of("hello", "world");

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

    // then
    subscriber.waitUntilAnyMessageReceived();
    List<ReceivedMessage> allReceivedMessages = subscriber.getAllReceivedMessages();
    assertThat(allReceivedMessages).hasSize(1);
    assertThat(allReceivedMessages.get(0).getMessage())
        .hasAttribute("tn")
        .hasAttribute("id")
        .hasAttribute("ts")
        .hasBody(message.body());
  }
}
