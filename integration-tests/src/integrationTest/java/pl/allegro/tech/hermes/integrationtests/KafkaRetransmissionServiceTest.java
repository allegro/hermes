package pl.allegro.tech.hermes.integrationtests;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.OffsetRetransmissionDate;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.supervisor.process.RunningSubscriptionStatus;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCOffsetChangeSummary;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.COMMIT;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class KafkaRetransmissionServiceTest {

    private static final String PRIMARY_KAFKA_CLUSTER_NAME = "primary-dc";

    private final AvroUser avroUser = new AvroUser();

    private final Clock clock = Clock.systemDefaultZone();

    private final List<String> messages = new ArrayList<>() {{
        range(0, 4).forEach(i -> add(TestMessage.random().body()));
    }};

    private final List<String> messages2 = new ArrayList<>() {{
        range(5, 6).forEach(i -> add(TestMessage.random().body()));
    }};

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    @RegisterExtension
    public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

    @Test
    public void shouldMoveOffsetNearGivenTimestamp() throws InterruptedException {
        // given
        final TestSubscriber subscriber = subscribers.createSubscriber();
        final Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        final Subscription subscription = hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint()).build());
        publishAndConsumeMessages(messages, topic, subscriber);
        Thread.sleep(1000); //wait 1s because our date time format has seconds precision
        final OffsetRetransmissionDate retransmissionDate = new OffsetRetransmissionDate(OffsetDateTime.now());
        Thread.sleep(1000);
        publishAndConsumeMessages(messages2, topic, subscriber);
        waitUntilConsumerCommitsOffset(topic.getQualifiedName(), subscription.getName());

        // when
        WebTestClient.ResponseSpec response = hermes.api().retransmit(topic.getQualifiedName(), subscription.getName(), retransmissionDate, false);

        // then
        response.expectStatus().isOk();
        messages2.forEach(subscriber::waitUntilReceived);
    }

    @Test
    public void shouldMoveOffsetInDryRunMode() throws InterruptedException {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        final Subscription subscription = hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint()).build());
        // 4 messages
        publishAndConsumeMessages(messages, topic, subscriber);
        Thread.sleep(2000);
        final OffsetRetransmissionDate retransmissionDate = new OffsetRetransmissionDate(OffsetDateTime.now());
        publishAndConsumeMessages(messages2, topic, subscriber);
        waitUntilConsumerCommitsOffset(topic.getQualifiedName(), subscription.getName());
        subscriber.reset();

        // when
        WebTestClient.ResponseSpec response = hermes.api().retransmit(topic.getQualifiedName(), subscription.getName(), retransmissionDate, true);

        // then
        response.expectStatus().isOk();
        MultiDCOffsetChangeSummary summary = response.expectBody(MultiDCOffsetChangeSummary.class).returnResult().getResponseBody();

        assert summary != null;
        Long offsetSum = summary.getPartitionOffsetListPerBrokerName().get(PRIMARY_KAFKA_CLUSTER_NAME).stream()
                .collect(Collectors.summarizingLong(PartitionOffset::getOffset)).getSum();
        assertThat(offsetSum).isEqualTo(4);
        subscriber.noMessagesReceived();
    }

    @Test
    public void shouldMoveOffsetInDryRunModeForTopicsMigratedToAvro() throws InterruptedException {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        Subscription subscription = hermes.initHelper().createSubscription(subscriptionWithRandomName(topic.getName(), subscriber.getEndpoint()).build());

        hermes.api().publish(topic.getQualifiedName(), TestMessage.simple().body());
        waitUntilConsumerCommitsOffset(topic.getQualifiedName(), subscription.getName());

        Thread.sleep(1000); //wait 1s because our date time format has seconds precision
        final OffsetRetransmissionDate retransmissionDate = new OffsetRetransmissionDate(OffsetDateTime.now());

        hermes.api().publish(topic.getQualifiedName(), TestMessage.simple().body());
        waitUntilConsumerCommitsOffset(topic.getQualifiedName(), subscription.getName());

        PatchData patch = patchData()
                .set("contentType", ContentType.AVRO)
                .set("migratedFromJsonType", true)
                .set("schema", avroUser.getSchemaAsString())
                .build();

        hermes.api().updateTopic(topic.getQualifiedName(), patch).expectStatus().isOk();

        hermes.api().publishAvroUntilSuccess(topic.getQualifiedName(), avroUser.asBytes());
        waitUntilConsumerCommitsOffset(topic.getQualifiedName(), subscription.getName());

        // when
        WebTestClient.ResponseSpec response = hermes.api().retransmit(topic.getQualifiedName(), subscription.getName(), retransmissionDate, true);

        // then
        response.expectStatus().isOk();
        MultiDCOffsetChangeSummary summary = response.expectBody(MultiDCOffsetChangeSummary.class).returnResult().getResponseBody();
        assert summary != null;
        PartitionOffsetsPerKafkaTopic offsets = PartitionOffsetsPerKafkaTopic.from(
                summary.getPartitionOffsetListPerBrokerName().get(PRIMARY_KAFKA_CLUSTER_NAME)
        );
        assertThat((Long) offsets.jsonPartitionOffsets.stream().mapToLong(PartitionOffset::getOffset).sum()).isEqualTo(1);
        assertThat((Long) offsets.avroPartitionOffsets.stream().mapToLong(PartitionOffset::getOffset).sum()).isEqualTo(0);
    }

    private void publishAndConsumeMessages(List<String> messages, Topic topic, TestSubscriber subscriber) {
        messages.forEach(message -> hermes.api().publish(topic.getQualifiedName(), message));
        messages.forEach(subscriber::waitUntilReceived);
    }

    private void waitUntilConsumerCommitsOffset(String topicQualifiedName, String subscription) {
        long currentTime = clock.millis();
        until(Duration.ofMinutes(1), topicQualifiedName, subscription, sub ->
                sub.getSignalTimesheet().getOrDefault(COMMIT, 0L) > currentTime);
    }

    private void until(Duration duration, String topicQualifiedName, String subscription, Predicate<RunningSubscriptionStatus> predicate) {
        waitAtMost(adjust(duration)).until(() ->
                hermes.api().getRunningSubscriptionsStatus().stream()
                        .filter(sub -> sub.getQualifiedName().equals(topicQualifiedName + "$" + subscription))
                        .anyMatch(predicate));
    }

    private record PartitionOffsetsPerKafkaTopic(List<PartitionOffset> avroPartitionOffsets,
                                                 List<PartitionOffset> jsonPartitionOffsets) {

        private static PartitionOffsetsPerKafkaTopic from(List<PartitionOffset> all) {
                ImmutableListMultimap<Boolean, PartitionOffset> partitionOffsets = Multimaps.index(
                        all, p -> p.getTopic().asString().endsWith("_avro")
                );
                return new PartitionOffsetsPerKafkaTopic(partitionOffsets.get(true), partitionOffsets.get(false));
            }
        }
}
