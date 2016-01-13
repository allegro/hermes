package pl.allegro.tech.hermes.integration;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.integration.env.HermesIntegrationEnvironment;
import pl.allegro.tech.hermes.integration.helper.Waiter;
import pl.allegro.tech.hermes.integration.shame.Unreliable;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesAPIOperations;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCOffsetChangeSummary;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.stream.Collectors.summingLong;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.integration.env.SharedServices.services;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.message.TestMessage.simpleMessages;

public class KafkaRetransmissionServiceTest extends HermesIntegrationEnvironment {

    private HermesEndpoints endpoints;
    private HermesPublisher publisher;
    private HermesAPIOperations operations;
    private RemoteServiceEndpoint remoteService;
    private Waiter wait;
    private AvroUser user;

    @BeforeClass
    public void initialize() throws IOException {
        publisher = new HermesPublisher(FRONTEND_URL);
        endpoints = new HermesEndpoints(MANAGEMENT_ENDPOINT_URL);
        wait = new Waiter(endpoints, services().zookeeper(), services().kafkaZookeeper(), KAFKA_NAMESPACE);
        operations = new HermesAPIOperations(endpoints, wait);
        user = new AvroUser();
    }

    @BeforeMethod
    public void initializeAlways() {
        remoteService = new RemoteServiceEndpoint(services().serviceMock());
    }

    @Test(enabled = false)
    @Unreliable
    public void shouldMoveOffsetNearGivenTimestamp() throws InterruptedException {
        // given
        String subscription = "subscription";

        Topic topic = operations.buildTopic("resetOffsetGroup", "topic");
        operations.createSubscription(topic, subscription, HTTP_ENDPOINT_URL);

        sendMessagesOnTopic(topic, 4);
        Thread.sleep(1000); //wait 1s because our date time format has seconds precision
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Thread.sleep(1000);
        sendMessagesOnTopic(topic, 2);
        wait.untilConsumerCommitsOffset();

        // when
        remoteService.expectMessages(simpleMessages(2));
        Response response = endpoints.subscription().retransmit(topic.getQualifiedName(), subscription, false, dateTime);
        wait.untilSubscriptionEndsReiteration(topic, subscription);

        // then
        assertThat(response).hasStatus(Response.Status.OK);
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldMoveOffsetInDryRunMode() throws InterruptedException {
        // given
        String subscription = "subscription";

        Topic topic = operations.buildTopic("resetOffsetGroup", "topicDryRun");
        operations.createSubscription(topic, subscription, HTTP_ENDPOINT_URL);

        sendMessagesOnTopic(topic, 4);
        Thread.sleep(1000); //wait 1s because our date time format has seconds precision
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        sendMessagesOnTopic(topic, 2);
        wait.untilConsumerCommitsOffset();

        // when
        Response response = endpoints.subscription().retransmit(topic.getQualifiedName(), subscription, true, dateTime);

        // then
        assertThat(response).hasStatus(Response.Status.OK);
        MultiDCOffsetChangeSummary summary = response.readEntity(MultiDCOffsetChangeSummary.class);

        assertThat(summary.getPartitionOffsetListPerBrokerName().get(PRIMARY_KAFKA_CLUSTER_NAME).get(0).getOffset()).isEqualTo(2);
        remoteService.makeSureNoneReceived();
    }

    @Test
    public void shouldMoveOffsetInDryRunModeForTopicsMigratedToAvro() throws InterruptedException, IOException {
        // given
        String subscription = "subscription";

        Topic topic = operations.buildTopic("resetOffsetGroup", "migratedTopicDryRun");
        operations.createSubscription(topic, subscription, HTTP_ENDPOINT_URL);

        sendMessagesOnTopic(topic, 1);
        Thread.sleep(1000); //wait 1s because our date time format has seconds precision
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        sendMessagesOnTopic(topic, 1);
        wait.untilConsumerCommitsOffset();

        Topic migratedTopic = topic()
                .applyPatch(topic)
                .withContentType(AVRO)
                .withMessageSchema(user.getSchema().toString())
                .migratedFromJsonType()
                .build();
        operations.updateTopic("resetOffsetGroup", "migratedTopicDryRun", migratedTopic);

        sendAvroMessageOnTopic(topic, user.asTestMessage());

        wait.untilConsumerCommitsOffset();

        // when
        Response response = endpoints.subscription().retransmit(topic.getQualifiedName(), subscription, true, dateTime);

        // then
        assertThat(response).hasStatus(Response.Status.OK);
        MultiDCOffsetChangeSummary summary = response.readEntity(MultiDCOffsetChangeSummary.class);
        PartitionOffsetsPerKafkaTopic offsets = PartitionOffsetsPerKafkaTopic.from(summary.getPartitionOffsetListPerBrokerName().get(PRIMARY_KAFKA_CLUSTER_NAME));

        assertThat(offsets.jsonPartitionOffsets.stream().collect(summingLong(PartitionOffset::getOffset))).isEqualTo(1);
        assertThat(offsets.avroPartitionOffsets.stream().collect(summingLong(PartitionOffset::getOffset))).isEqualTo(0);
    }

    private void sendAvroMessageOnTopic(Topic topic, TestMessage afterMigrationMessage) {
        remoteService.expectMessages(afterMigrationMessage);
        publisher.publish(topic.getQualifiedName(), afterMigrationMessage.withEmptyAvroMetadata().body());
        remoteService.waitUntilReceived(120);
        remoteService.reset();
    }

    private void sendMessagesOnTopic(Topic topic, int n) {
        remoteService.expectMessages(simpleMessages(n));
        for (TestMessage message: simpleMessages(n)) {
            publisher.publish(topic.getQualifiedName(), message.body());
        }
        remoteService.waitUntilReceived();
        remoteService.reset();
    }

    private static class PartitionOffsetsPerKafkaTopic {
        private final List<PartitionOffset> avroPartitionOffsets;
        private final List<PartitionOffset> jsonPartitionOffsets;

        private PartitionOffsetsPerKafkaTopic(List<PartitionOffset> avroPartitionOffsets, List<PartitionOffset> jsonPartitionOffsets) {
            this.avroPartitionOffsets = avroPartitionOffsets;
            this.jsonPartitionOffsets = jsonPartitionOffsets;
        }

        private static PartitionOffsetsPerKafkaTopic from(List<PartitionOffset> all) {
            ImmutableListMultimap<Boolean, PartitionOffset> partitionOffsets = Multimaps.index(
                    all, p -> p.getTopic().asString().endsWith("_avro")
            );
            return new PartitionOffsetsPerKafkaTopic(partitionOffsets.get(true), partitionOffsets.get(false));
        }

    }
}

