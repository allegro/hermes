package pl.allegro.tech.hermes.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class BasicAuthSubscribingTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @RegisterExtension
  public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

  @Test
  public void shouldAuthorizeUsingBasicAuthWhenSubscriptionHasCredentials() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();

    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    hermes
        .initHelper()
        .createSubscription(
            subscription(
                    topic.getQualifiedName(),
                    "subscription",
                    "http://user:password@localhost:" + subscriber.getPort() + subscriber.getPath())
                .build());

    TestMessage message = TestMessage.of("hello", "world");

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

    // then
    subscriber.waitUntilMessageWithHeaderReceived("Authorization", "Basic dXNlcjpwYXNzd29yZA==");
  }

  @Test
  public void shouldUpdateSubscriptionUsernameAndPassword() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    hermes
        .initHelper()
        .createSubscription(
            subscription(
                    topic.getQualifiedName(), "subscription", "http://user:password@localhost:1234")
                .build());

    // when
    hermes
        .api()
        .updateSubscription(
            topic,
            "subscription",
            patchData().set("endpoint", "http://newuser:newpassword@localhost:1234").build());

    // then
    Subscription subscription =
        hermes.api().getSubscription(topic.getQualifiedName(), "subscription");
    assertThat(subscription.getEndpoint().getUsername()).isEqualTo("newuser");
  }
}
