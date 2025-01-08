package pl.allegro.tech.hermes.integrationtests.management;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Stats;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.api.TrackingMode;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;

public class StatsTest {

  private static final String SCHEMA = AvroUserSchemaLoader.load().toString();

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @Test
  public void shouldGetStats() {
    // given
    Stats initalStats = getStats();

    TopicWithSchema topic1 = topicWithSchema(topic(Topic.Ack.ALL, ContentType.AVRO, true), SCHEMA);
    Topic topic2 = topic(Topic.Ack.LEADER, ContentType.JSON, false);

    hermes.initHelper().createTopicWithSchema(topic1);
    hermes.initHelper().createTopic(topic2);
    hermes.initHelper().createTopic(topic(Topic.Ack.ALL, ContentType.JSON, true));

    hermes
        .initHelper()
        .createSubscription(
            subscription(topic1.getQualifiedName(), ContentType.AVRO, TrackingMode.TRACKING_OFF));

    hermes
        .initHelper()
        .createSubscription(
            subscription(topic2.getQualifiedName(), ContentType.JSON, TrackingMode.TRACK_ALL));

    hermes
        .initHelper()
        .createSubscription(
            subscription(topic2.getQualifiedName(), ContentType.AVRO, TrackingMode.TRACKING_OFF));

    // when
    Stats stats = getStats();

    // then
    assertThat(stats.getTopicStats().getTopicCount())
        .isEqualTo(initalStats.getTopicStats().getTopicCount() + 3);
    assertThat(stats.getTopicStats().getAckAllTopicCount())
        .isEqualTo(initalStats.getTopicStats().getAckAllTopicCount() + 2);
    assertThat(stats.getTopicStats().getTrackingEnabledTopicCount())
        .isEqualTo(initalStats.getTopicStats().getTrackingEnabledTopicCount() + 2);
    assertThat(stats.getTopicStats().getAvroTopicCount())
        .isEqualTo(initalStats.getTopicStats().getAvroTopicCount() + 1);

    assertThat(stats.getSubscriptionStats().getSubscriptionCount())
        .isEqualTo(initalStats.getSubscriptionStats().getSubscriptionCount() + 3);
    assertThat(stats.getSubscriptionStats().getTrackingEnabledSubscriptionCount())
        .isEqualTo(initalStats.getSubscriptionStats().getTrackingEnabledSubscriptionCount() + 1);
    assertThat(stats.getSubscriptionStats().getAvroSubscriptionCount())
        .isEqualTo(initalStats.getSubscriptionStats().getAvroSubscriptionCount() + 2);
  }

  private Topic topic(Topic.Ack ack, ContentType contentType, boolean trackingEnabled) {
    return topicWithRandomName()
        .withAck(ack)
        .withContentType(contentType)
        .withTrackingEnabled(trackingEnabled)
        .build();
  }

  private Subscription subscription(
      String topicQualifiedName, ContentType contentType, TrackingMode trackingMode) {
    return SubscriptionBuilder.subscription(topicQualifiedName, UUID.randomUUID().toString())
        .withContentType(contentType)
        .withTrackingMode(trackingMode)
        .build();
  }

  private Stats getStats() {
    return hermes
        .api()
        .getManagementStats()
        .expectStatus()
        .isOk()
        .expectBody(Stats.class)
        .returnResult()
        .getResponseBody();
  }
}
