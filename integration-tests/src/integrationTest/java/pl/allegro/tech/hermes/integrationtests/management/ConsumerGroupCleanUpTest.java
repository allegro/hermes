package pl.allegro.tech.hermes.integrationtests.management;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;

public class ConsumerGroupCleanUpTest {
  @RegisterExtension
  public static final HermesExtension hermes =
      new HermesExtension()
          .withManagementArgs(
              "--consumer-group.clean-up.enabled=true",
              "--consumer-group.clean-up.interval=PT5S",
              "--consumer-group.clean-up.initial-delay=PT0S",
              "--consumer-group.clean-up.timeout=PT20S");

  @Test
  public void shouldRemoveConsumerGroupAfterSubscriptionRemoval() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    Subscription subscription =
        hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName()).build());

    // and
    assertThat(hermes.countConsumerGroups(topic)).isNotEqualTo(0);

    // when
    hermes.api().deleteSubscription(topic.getQualifiedName(), subscription.getName());

    // then
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> assertThat(hermes.countConsumerGroups(topic)).isEqualTo(0));
  }

  @Test
  public void shouldRemoveConsumerGroupAfterTopicRemoval() {
    // given
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName())
                .withAutoDeleteWithTopicEnabled(true)
                .build());

    // and
    assertThat(hermes.countConsumerGroups(topic)).isNotEqualTo(0);

    // when
    hermes.api().deleteTopic(topic.getQualifiedName());

    // then
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> assertThat(hermes.countConsumerGroups(topic)).isEqualTo(0));
  }
}
