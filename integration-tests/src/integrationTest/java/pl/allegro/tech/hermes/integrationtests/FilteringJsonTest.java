package pl.allegro.tech.hermes.integrationtests;

import static com.google.common.collect.ImmutableMap.of;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

public class FilteringJsonTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @RegisterExtension
  public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

  private static final MessageFilterSpecification MESSAGE_NAME_FILTER =
      new MessageFilterSpecification(of("type", "jsonpath", "path", ".name", "matcher", "^Bob.*"));

  private static final MessageFilterSpecification MESSAGE_COLOR_FILTER =
      new MessageFilterSpecification(
          of("type", "jsonpath", "path", ".favoriteColor", "matcher", "grey"));

  static final AvroUser BOB = new AvroUser("Bob", 50, "blue");
  static final AvroUser ALICE = new AvroUser("Alice", 20, "magenta");
  static final AvroUser ALICE_GREY = new AvroUser("Alice", 20, "grey");
  static final AvroUser BOB_GREY = new AvroUser("Bob", 50, "grey");

  private static final SubscriptionPolicy SUBSCRIPTION_POLICY =
      new SubscriptionPolicy(100, 2000, 1000, 1000, true, 100, null, 0, 1, 600);

  @Test
  public void shouldFilterIncomingEvents() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint())
                .withSubscriptionPolicy(SUBSCRIPTION_POLICY)
                .withFilter(MESSAGE_NAME_FILTER)
                .build());

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), ALICE.asJson());
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), BOB.asJson());

    // then
    subscriber.waitUntilAllReceivedStrict(Set.of(BOB.asJson()));
  }

  @Test
  public void shouldChainFilters() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint())
                .withSubscriptionPolicy(SUBSCRIPTION_POLICY)
                .withFilter(MESSAGE_NAME_FILTER)
                .withFilter(MESSAGE_COLOR_FILTER)
                .build());

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), ALICE.asJson());
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), ALICE_GREY.asJson());
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), BOB.asJson());
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), BOB_GREY.asJson());

    // then
    subscriber.waitUntilAllReceivedStrict(Set.of(BOB_GREY.asJson()));
  }

  @Test
  public void shouldPassSubscriptionHeadersWhenFilteringIsEnabledForIncomingEvents() {
    // given
    TestSubscriber subscriber = subscribers.createSubscriber();
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    hermes
        .initHelper()
        .createSubscription(
            subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint())
                .withSubscriptionPolicy(SUBSCRIPTION_POLICY)
                .withFilter(MESSAGE_NAME_FILTER)
                .withHeader("MY-HEADER", "myHeaderValue")
                .build());

    // when
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), ALICE.asJson());
    hermes.api().publishUntilSuccess(topic.getQualifiedName(), BOB.asJson());

    // then
    subscriber.waitUntilAllReceivedStrict(Set.of(BOB.asJson()));
    subscriber.waitUntilMessageWithHeaderReceived("MY-HEADER", "myHeaderValue");
  }
}
