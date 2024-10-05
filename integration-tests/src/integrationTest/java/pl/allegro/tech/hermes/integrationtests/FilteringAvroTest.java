package pl.allegro.tech.hermes.integrationtests;

import static com.google.common.collect.ImmutableMap.of;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

public class FilteringAvroTest {

  private static final MessageFilterSpecification MESSAGE_NAME_FILTER =
      new MessageFilterSpecification(of("type", "avropath", "path", ".name", "matcher", "Bob"));

  private static final MessageFilterSpecification MESSAGE_COLOR_FILTER =
      new MessageFilterSpecification(
          of("type", "avropath", "path", ".favoriteColor", "matcher", "grey"));

  private static final AvroUser BOB = new AvroUser("Bob", 50, "blue");
  private static final AvroUser ALICE = new AvroUser("Alice", 20, "magenta");
  private static final AvroUser BOB_GREY = new AvroUser("Bob", 50, "grey");

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @RegisterExtension
  public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

  @Test
  public void shouldFilterIncomingEvents() {
    // given
    TopicWithSchema topicWithSchema =
        topicWithSchema(
            topicWithRandomName().withContentType(AVRO).build(), BOB.getSchemaAsString());

    Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

    TestSubscriber subscriber = subscribers.createSubscriber();
    final Subscription subscription =
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint())
            .withContentType(ContentType.JSON)
            .withFilter(MESSAGE_NAME_FILTER)
            .build();

    hermes.initHelper().createSubscription(subscription);

    // when
    hermes.api().publishJSONUntilSuccess(topic.getQualifiedName(), ALICE.asJson());
    hermes.api().publishJSONUntilSuccess(topic.getQualifiedName(), BOB.asJson());

    // then
    subscriber.waitUntilAllReceivedStrict(Set.of(BOB.asJson()));
  }

  @Test
  public void shouldChainMultipleFilters() {
    // given
    TopicWithSchema topicWithSchema =
        topicWithSchema(
            topicWithRandomName().withContentType(AVRO).build(), BOB.getSchemaAsString());

    Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

    TestSubscriber subscriber = subscribers.createSubscriber();
    final Subscription subscription =
        subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint())
            .withContentType(ContentType.JSON)
            .withFilter(MESSAGE_NAME_FILTER)
            .withFilter(MESSAGE_COLOR_FILTER)
            .build();

    hermes.initHelper().createSubscription(subscription);

    // when
    hermes.api().publishJSONUntilSuccess(topic.getQualifiedName(), BOB.asJson());
    hermes.api().publishJSONUntilSuccess(topic.getQualifiedName(), BOB_GREY.asJson());

    // then
    subscriber.waitUntilAllReceivedStrict(Set.of(BOB_GREY.asJson()));
  }
}
