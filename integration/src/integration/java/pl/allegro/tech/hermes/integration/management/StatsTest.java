package pl.allegro.tech.hermes.integration.management;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Stats;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionStats;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.TopicStats;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.api.TrackingMode;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;

import java.util.UUID;

public class StatsTest extends IntegrationTest {
    private static final String group = "statsGroup";

    @Test
    public void shouldGetStats() {
        // given
        operations.createGroup(group);
        TopicWithSchema topic1 = operations.createTopic(topic(group, name(), Topic.Ack.ALL, ContentType.AVRO, true));
        TopicWithSchema topic2 = operations.createTopic(topic(group, name(), Topic.Ack.LEADER, ContentType.JSON, false));
        operations.createTopic(topic(group, name(), Topic.Ack.ALL, ContentType.JSON, true));

        operations.createSubscription(topic1.getTopic(), subscription(topic1.getName(), name(), ContentType.AVRO, TrackingMode.TRACKING_OFF));
        operations.createSubscription(topic2.getTopic(), subscription(topic1.getName(), name(), ContentType.JSON, TrackingMode.TRACK_ALL));
        operations.createSubscription(topic2.getTopic(), subscription(topic1.getName(), name(), ContentType.AVRO, TrackingMode.TRACKING_OFF));

        // when then
        Assertions.assertThat(management.statsEndpoint().getStats()).isEqualTo(new Stats(
                new TopicStats(3L, 2L, 2L, 1L),
                new SubscriptionStats(3L, 1L, 2L)
        ));
    }

    private Topic topic(String groupName, String topicName, Topic.Ack ack, ContentType contentType, boolean trackingEnabled) {
        return TopicBuilder.topic(groupName, topicName)
                .withAck(ack)
                .withContentType(contentType)
                .withTrackingEnabled(trackingEnabled)
                .build();
    }

    private Subscription subscription(TopicName topicName, String subscriptionName, ContentType contentType, TrackingMode trackingMode) {
        return SubscriptionBuilder.subscription(topicName, subscriptionName)
                .withContentType(contentType)
                .withTrackingMode(trackingMode)
                .build();
    }

    private String name() {
        return UUID.randomUUID().toString();
    }

}
