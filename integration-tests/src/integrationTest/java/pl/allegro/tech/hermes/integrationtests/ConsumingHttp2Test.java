package pl.allegro.tech.hermes.integrationtests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class ConsumingHttp2Test {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @RegisterExtension
  public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

  @Test
  public void shouldDeliverMessageUsingHttp2() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    hermes
        .initHelper()
        .createSubscription(
            subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint())
                .withHttp2Enabled(true)
                .build());
    TestMessage message = TestMessage.of("hello", "world");

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

    // then
    subscriber.waitUntilReceived(message.body());
    LoggedRequest lastReceivedRequest = subscriber.getLastReceivedRequest();
    assertThat(lastReceivedRequest.getHeader("Keep-Alive")).isNull();
    assertThat(lastReceivedRequest.getProtocol()).isEqualTo("HTTP/2.0");
  }
}
