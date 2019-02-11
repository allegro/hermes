package pl.allegro.tech.hermes.integration;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.integration.env.ManagementStarter;
import pl.allegro.tech.hermes.integration.helper.Waiter;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCOffsetChangeSummary;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesAPIOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.core.Response;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.stream.Collectors.summingLong;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.OK;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.integration.env.SharedServices.services;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;
import static pl.allegro.tech.hermes.test.helper.message.TestMessage.simpleMessages;

public class KafkaRetransmissionServiceTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;
    private final AvroUser user = new AvroUser();
    private Clock clock = Clock.systemDefaultZone();

    private static String RETRANSMISSION_ENV = "retransmission";
    private static int MANAGEMENT_PORT = 18083;
    private static String MANAGEMENT_ENDPOINT_URL = "http://localhost:" + MANAGEMENT_PORT + "/";

    private ManagementStarter managementStarter;
    private HermesEndpoints management;
    private HermesAPIOperations operations;
    private Waiter wait;

    @BeforeMethod
    public void initializeAlways() {
        remoteService = new RemoteServiceEndpoint(services().serviceMock());
    }

    @BeforeClass
    void beforeClass() throws Exception {
        managementStarter = new ManagementStarter(MANAGEMENT_PORT, RETRANSMISSION_ENV);
        managementStarter.start();
        management = new HermesEndpoints(MANAGEMENT_ENDPOINT_URL, CONSUMER_ENDPOINT_URL);
        wait = new Waiter(management, services().zookeeper(), brokerOperations, PRIMARY_KAFKA_CLUSTER_NAME, KAFKA_NAMESPACE);
        operations = new HermesAPIOperations(management, wait);
    }

    @Test
    public void shouldMoveOffsetNearGivenTimestamp() throws InterruptedException {
        // given
        String subscription = "subscription";

        Topic topic = operations.buildTopic(randomTopic("resetOffsetGroup", "topic").build());
        operations.createSubscription(topic, subscription, HTTP_ENDPOINT_URL);
        operations.createSubscription(topic, "subscriptionSuspended", HTTP_ENDPOINT_URL, Subscription.State.SUSPENDED);

        sendMessagesOnTopic(topic, 4);
        Thread.sleep(1000); //wait 1s because our date time format has seconds precision
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Thread.sleep(1000);
        sendMessagesOnTopic(topic, 2);
        wait.untilConsumerCommitsOffset(topic, subscription);

        // when
        remoteService.expectMessages(simpleMessages(2));
        Response response = management.subscription().retransmit(topic.getQualifiedName(), subscription, false, dateTime);
        wait.untilSubscriptionEndsReiteration(topic, subscription);

        // then
        assertThat(response).hasStatus(OK);
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldMoveOffsetInDryRunMode() throws InterruptedException {
        // given
        String subscription = "subscription";

        Topic topic = operations.buildTopic(randomTopic("resetOffsetGroup", "topicDryRun").build());
        operations.createSubscription(topic, subscription, HTTP_ENDPOINT_URL);

        // we have 2 partitions, thus 4 messages to get 2 per partition
        sendMessagesOnTopic(topic, 4);
        Thread.sleep(2000); //wait 1s because our date time format has seconds precision
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        sendMessagesOnTopic(topic, 2);
        wait.untilConsumerCommitsOffset(topic, subscription);

        // when
        Response response = management.subscription().retransmit(topic.getQualifiedName(), subscription, true, dateTime);

        // then
        assertThat(response).hasStatus(OK);
        MultiDCOffsetChangeSummary summary = response.readEntity(MultiDCOffsetChangeSummary.class);

        assertThat(summary.getPartitionOffsetListPerBrokerName().get(PRIMARY_KAFKA_CLUSTER_NAME).get(0).getOffset())
                .isEqualTo(2);
        remoteService.makeSureNoneReceived();
    }

    @Test
    public void shouldMoveOffsetInDryRunModeForTopicsMigratedToAvro() throws InterruptedException {
        // given
        Topic topic = operations.buildTopic(randomTopic("resetOffsetGroup", "migratedTopicDryRun").build());
        long currentTime = clock.millis();
        Subscription subscription = operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        operations.createSubscription(topic, "subscriptionSuspended", HTTP_ENDPOINT_URL, Subscription.State.SUSPENDED);

        wait.untilSubscriptionIsActivated(currentTime, topic, subscription.getName());

        sendMessagesOnTopic(topic, 1);
        wait.untilConsumerCommitsOffset(topic, subscription.getName());

        Thread.sleep(1000); //wait 1s because our date time format has seconds precision
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        sendMessagesOnTopic(topic, 1);
        wait.untilConsumerCommitsOffset(topic, subscription.getName());

        PatchData patch = patchData()
                .set("contentType", ContentType.AVRO)
                .set("migratedFromJsonType", true)
                .set("schema", user.getSchemaAsString())
                .build();

        currentTime = clock.millis();
        operations.updateTopic(topic.getName().getGroupName(), topic.getName().getName(), patch);
        wait.untilTopicIsUpdatedAfter(currentTime, topic, subscription.getName());

        sendAvroMessageOnTopic(topic, user.asTestMessage());
        wait.untilConsumerCommitsOffset(topic, subscription.getName());

        // when
        Response response = management.subscription().retransmit(topic.getQualifiedName(), subscription.getName(), true, dateTime);

        // then
        assertThat(response).hasStatus(OK);
        MultiDCOffsetChangeSummary summary = response.readEntity(MultiDCOffsetChangeSummary.class);
        PartitionOffsetsPerKafkaTopic offsets = PartitionOffsetsPerKafkaTopic.from(
                summary.getPartitionOffsetListPerBrokerName().get(PRIMARY_KAFKA_CLUSTER_NAME)
        );

        assertThat(offsets.jsonPartitionOffsets.stream().collect(summingLong(PartitionOffset::getOffset))).isEqualTo(1);
        assertThat(offsets.avroPartitionOffsets.stream().collect(summingLong(PartitionOffset::getOffset))).isEqualTo(0);
    }

    @AfterClass
    public void afterClass() throws Exception {
        managementStarter.stop();
    }

    private void sendAvroMessageOnTopic(Topic topic, TestMessage message) {
        remoteService.expectMessages(message);
        Response response = publisher.publish(topic.getQualifiedName(), message.body());
        assertThat(response).hasStatus(Response.Status.CREATED);
        remoteService.waitUntilReceived(60);
        remoteService.reset();
    }

    private void sendMessagesOnTopic(Topic topic, int n) {
        remoteService.expectMessages(simpleMessages(n));
        for (TestMessage message : simpleMessages(n)) {
            assertThat(publisher.publish(topic.getQualifiedName(), message.body()).getStatusInfo().getFamily()).isEqualTo(SUCCESSFUL);
        }
        remoteService.waitUntilReceived();
        remoteService.reset();
    }

    private static class PartitionOffsetsPerKafkaTopic {
        private final List<PartitionOffset> avroPartitionOffsets;
        private final List<PartitionOffset> jsonPartitionOffsets;

        private PartitionOffsetsPerKafkaTopic(
                List<PartitionOffset> avroPartitionOffsets,
                List<PartitionOffset> jsonPartitionOffsets
        ) {
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

