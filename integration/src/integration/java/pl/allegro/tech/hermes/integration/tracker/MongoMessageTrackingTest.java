package pl.allegro.tech.hermes.integration.tracker;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.BatchSubscriptionPolicy;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.client.HermesClient;
import pl.allegro.tech.hermes.client.jersey.JerseyHermesSender;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.integration.env.FongoFactory;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.shame.Unreliable;
import pl.allegro.tech.hermes.integration.test.HermesAssertions;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.tracker.mongo.LogSchemaAware;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static javax.ws.rs.client.ClientBuilder.newClient;
import static pl.allegro.tech.hermes.api.BatchSubscriptionPolicy.Builder.batchSubscriptionPolicy;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class MongoMessageTrackingTest extends IntegrationTest {

    public static final String INVALID_ENDPOINT_URL = "http://localhost:60000";
    public static final TestMessage MESSAGE = TestMessage.of("hello", "world");

    private DBCollection publishedMessages;
    private DBCollection sentMessages;
    private RemoteServiceEndpoint remoteService;
    private HermesClient client;
    private Clock clock = Clock.systemDefaultZone();

    @BeforeClass
    public void initialize() {
        DB mongo = FongoFactory.hermesDB();
        this.publishedMessages = mongo.getCollection(LogSchemaAware.COLLECTION_PUBLISHED_NAME);
        this.sentMessages = mongo.getCollection(LogSchemaAware.COLLECTION_SENT_NAME);
    }

    @BeforeMethod
    public void initializeAlways() {
        remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
        client = hermesClient(new JerseyHermesSender(newClient())).withURI(URI.create(CLIENT_FRONTEND_URL)).build();
        publishedMessages.remove(new BasicDBObject());
        sentMessages.remove(new BasicDBObject());
    }

    @AfterClass
    public void closeSetup() throws IOException {
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldLogMessagePublishing() {
        // given
        operations.buildTopic(topic("logMessagePublishing", "topic").withContentType(JSON).withTrackingEnabled(true).build());

        // when
        publisher.publish("logMessagePublishing.topic", MESSAGE.body());

        // then
        wait.untilMessageTraceLogged(publishedMessages, PublishedMessageTraceStatus.SUCCESS);
        assertThat(findAllStatusesByTopic(publishedMessages, "logMessagePublishing.topic")).contains("SUCCESS");
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldLogMessageInflightAndSending() {
        // given
        Topic topic = operations.buildTopic(topic("logMessageSending", "topic").withContentType(JSON).build());
        Subscription subscription = subscription("logMessageSending.topic", "subscription", HTTP_ENDPOINT_URL)
                .withTrackingEnabled(true)
                .build();

        operations.createSubscription(topic, subscription);
        remoteService.expectMessages(MESSAGE.body());

        // when
        publisher.publish("logMessageSending.topic", MESSAGE.body());
        wait.untilReceivedAnyMessage(sentMessages);

        // then
        wait.untilMessageTraceLogged(sentMessages, SentMessageTraceStatus.SUCCESS);

        assertThat(findAllStatusesByTopic(sentMessages, "logMessageSending.topic")).contains("INFLIGHT", "SUCCESS");
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldLogMessageDiscarding() {
        // given
        Topic topic = operations.buildTopic(topic("logMessageDiscarding", "topic").withContentType(JSON).build());
        Subscription subscription = subscription("logMessageDiscarding.topic", "subscription", INVALID_ENDPOINT_URL)
                .withTrackingEnabled(true)
                .withSubscriptionPolicy(subscriptionPolicy().withRate(1).withMessageTtl(3).build())
                .build();

        operations.createSubscription(topic, subscription);

        // when
        publisher.publish("logMessageDiscarding.topic", MESSAGE.body());

        // then
        wait.untilMessageTraceLogged(sentMessages, SentMessageTraceStatus.FAILED);
        wait.untilMessageTraceLogged(sentMessages, SentMessageTraceStatus.DISCARDED);
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldFetchUndeliveredMessagesLogs() {
        // given
        Topic topic = operations.buildTopic(topic("fetchUndeliveredMessagesLogs", "topic").withContentType(JSON).withTrackingEnabled(true).build());

        Subscription subscription = subscription("fetchUndeliveredMessagesLogs.topic", "subscription", INVALID_ENDPOINT_URL)
                .withTrackingEnabled(true)
                .withSupportTeam("team")
                .withSubscriptionPolicy(subscriptionPolicy().withRate(1).withMessageTtl(5).build())
                .build();

        operations.createSubscription(topic, subscription);

        publisher.publish("fetchUndeliveredMessagesLogs.topic", MESSAGE.body());
        wait.untilMessageTraceLogged(sentMessages, SentMessageTraceStatus.DISCARDED);

        // when
        Response response = management.subscription().getLatestUndeliveredMessages("fetchUndeliveredMessagesLogs.topic", "subscription");

        // then
        List<SentMessageTrace> messages = readSentMessages(response);
        assertThat(messages).extracting("status").contains(SentMessageTraceStatus.DISCARDED);
        assertThat(messages).extracting("topicName").containsOnly(TopicName.fromQualifiedName("fetchUndeliveredMessagesLogs.topic"));
        assertThat(messages).extracting("partition").isNotEmpty();
        assertThat(messages).extracting("offset").isNotEmpty();
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldToggleTrackingOnTopicUpdate() {
        // given
        TopicName topicName = new TopicName("toggleTrackingOnTopic", "topic");
        Topic topic = topic(topicName).withContentType(JSON).withTrackingEnabled(true).build();
        operations.buildTopic(topic);

        String firstTracked = publishMessage("toggleTrackingOnTopic.topic", MESSAGE.body());
        wait.untilMessageIdLogged(publishedMessages, firstTracked);

        // when
        operations.updateTopic(topicName, patchData().set("trackingEnabled", false).build());

        publishMessage("toggleTrackingOnTopic.topic", MESSAGE.body());

        operations.updateTopic(topicName, patchData().set("trackingEnabled", true).build());

        String secondTracked = publishMessage("toggleTrackingOnTopic.topic", MESSAGE.body());
        wait.untilMessageIdLogged(publishedMessages, secondTracked);

        // then
        assertThat(findAllMessageIdsByTopic(publishedMessages, "toggleTrackingOnTopic.topic")).containsOnly(firstTracked, secondTracked);
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldNotChangeAckWhenEnablingTrackingOnTopic() {
        // given
        TopicName topicName = new TopicName("ackStaysOnTracking", "topic");
        Topic topic = topic(topicName).withAck(Topic.Ack.ALL).withContentType(JSON).withTrackingEnabled(false).build();
        operations.buildTopic(topic);

        // when
        operations.updateTopic(topicName, patchData().set("trackingEnabled", true).build());

        Topic updatedTopic = operations.getTopic("ackStaysOnTracking", "topic");

        // then
        assertThat(updatedTopic.isTrackingEnabled()).isTrue();
        assertThat(updatedTopic.getAck()).isEqualTo(Topic.Ack.ALL);
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldToggleTrackingOnSubscriptionUpdate() {
        // given
        Topic topic = operations.buildTopic(topic("toggleTrackingOnSubscription", "topic").withContentType(JSON).build());
        Subscription subscription = subscription("toggleTrackingOnSubscription.topic", "subscription", HTTP_ENDPOINT_URL)
                .withTrackingEnabled(true)
                .build();

        operations.createSubscription(topic, subscription);
        remoteService.expectMessages(MESSAGE.body(), MESSAGE.body());
        String firstTracked = publishMessage("toggleTrackingOnSubscription.topic", MESSAGE.body());
        wait.untilMessageIdLogged(sentMessages, firstTracked);

        // when
        long currentTime = clock.millis();
        operations.updateSubscription("toggleTrackingOnSubscription", "topic", "subscription", patchData().set("trackingEnabled", false).build());

        wait.untilConsumersUpdateSubscription(currentTime, topic, subscription.getName());
        publishMessage("toggleTrackingOnSubscription.topic", MESSAGE.body());
        remoteService.waitUntilReceived();

        currentTime = clock.millis();
        operations.updateSubscription("toggleTrackingOnSubscription", "topic", "subscription", patchData().set("trackingEnabled", true).build());
        wait.untilConsumersUpdateSubscription(currentTime, topic, subscription.getName());

        remoteService.expectMessages(MESSAGE.body());
        String secondTracked = publishMessage("toggleTrackingOnSubscription.topic", MESSAGE.body());
        remoteService.waitUntilReceived();

        wait.untilMessageIdLogged(sentMessages, secondTracked);

        // then
        assertThat(findAllMessageIdsByTopic(sentMessages, "toggleTrackingOnSubscription.topic")).containsOnly(firstTracked, secondTracked);
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldReturnEmptyListIfThereAreNoSentMessages() {
        //given
        assertThat(sentMessages.count()).isZero();
        Topic topic = operations.buildTopic(topic("returnEmptyListIfThereAreNoSentMessages", "topic").withContentType(JSON).build());
        operations.createSubscription(topic, "subscription3", INVALID_ENDPOINT_URL);

        //when
        Response response = management.subscription().getLatestUndeliveredMessages(topic.getQualifiedName(), "subscription3");

        //then
        assertThat(readSentMessages(response)).isEmpty();
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldLogBatchIdInMessageTrace() {
        // given
        TestMessage message = TestMessage.simple();
        TestMessage[] batch = {message};
        Topic topic = operations.buildTopic(topic("logBatchIdInMessageTrace", "topic").withContentType(JSON).build());

        BatchSubscriptionPolicy subscriptionPolicy = batchSubscriptionPolicy()
                .withBatchSize(2)
                .withBatchVolume(100)
                .withBatchTime(1000)
                .withMessageTtl(100)
                .withMessageBackoff(10)
                .withRequestTimeout(100)
                .build();

        Subscription subscription = subscription("logBatchIdInMessageTrace.topic", "subscription", HTTP_ENDPOINT_URL)
                .withTrackingEnabled(true)
                .withSubscriptionPolicy(subscriptionPolicy)
                .build();

        operations.createSubscription(topic, subscription);
        remoteService.expectMessages(batch);

        // when
        String messageId1 = publishMessage("logBatchIdInMessageTrace.topic", message.body());
        String messageId2 = publishMessage("logBatchIdInMessageTrace.topic", message.body());
        wait.untilReceivedAnyMessage(sentMessages);

        // then
        wait.untilMessageIdLogged(sentMessages, messageId1);
        wait.untilMessageIdLogged(sentMessages, messageId2);

        assertThat(findAllBatchIdsByTopic(sentMessages, "logBatchSending.topic").get(messageId1))
                .isEqualTo(findAllBatchIdsByTopic(sentMessages, "logBatchSending.topic").get(messageId2));
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldLogBatchInflightAndSending() {
        // given
        TestMessage message = TestMessage.simple();
        TestMessage[] batch = {message};
        Topic topic = operations.buildTopic(topic("logBatchInflightAndSending", "topic").withContentType(JSON).build());

        BatchSubscriptionPolicy subscriptionPolicy = singleMessageBatchPolicy();

        Subscription subscription = subscription("logBatchInflightAndSending.topic", "subscription", HTTP_ENDPOINT_URL)
                .withTrackingEnabled(true)
                .withSubscriptionPolicy(subscriptionPolicy)
                .build();

        operations.createSubscription(topic, subscription);
        remoteService.expectMessages(batch);

        // when
        publishMessage("logBatchInflightAndSending.topic", message.body());

        // then
        wait.untilMessageTraceLogged(sentMessages, SentMessageTraceStatus.SUCCESS);
        assertThat(findAllStatusesByTopic(sentMessages, "logBatchInflightAndSending.topic")).contains("INFLIGHT", "SUCCESS");
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldLogBatchDiscarding() {
        // given
        Topic topic = operations.buildTopic(topic("logBatchDiscarding", "topic").withContentType(JSON).build());

        BatchSubscriptionPolicy subscriptionPolicy = batchSubscriptionPolicy()
                .withBatchSize(1)
                .withBatchVolume(200)
                .withBatchTime(1)
                .withMessageTtl(1)
                .withMessageBackoff(1000)
                .withRequestTimeout(1).build();

        Subscription subscription = subscription("logBatchDiscarding.topic", "subscription", INVALID_ENDPOINT_URL)
                .withTrackingEnabled(true)
                .withSubscriptionPolicy(subscriptionPolicy)
                .build();

        operations.createSubscription(topic, subscription);

        // when
        publisher.publish("logBatchDiscarding.topic", MESSAGE.body());

        // then
        wait.untilMessageTraceLogged(sentMessages, SentMessageTraceStatus.DISCARDED);
        assertThat(findAllStatusesByTopic(sentMessages, "logBatchDiscarding.topic")).contains("FAILED", "DISCARDED");
    }

    private List<SentMessageTrace> readSentMessages(Response response) {
        HermesAssertions.assertThat(response).hasStatus(Response.Status.OK);
        return asList(response.readEntity(SentMessageTrace[].class));
    }

    private List<String> findAllMessageIdsByTopic(DBCollection collection, String topicName) {
        return collection.find(new BasicDBObject("topicName", topicName)).toArray()
                .stream().map((dbo) -> dbo.get("messageId").toString()).collect(Collectors.toList());
    }

    private Map<String, String> findAllBatchIdsByTopic(DBCollection collection, String topicName) {
        Map<String, String> ids = new HashMap<>();

        collection.find(new BasicDBObject("topicName", topicName)).toArray().forEach(dbObject -> {
            ids.putIfAbsent(dbObject.get("messageId").toString(), dbObject.get("batchId").toString());
        });

        return ids;
    }

    private List<String> findAllStatusesByTopic(DBCollection collection, String topicName) {
        return collection.find(new BasicDBObject("topicName", topicName)).toArray()
                .stream().map((dbo) -> dbo.get("status").toString()).collect(Collectors.toList());
    }

    private String publishMessage(String qualifiedTopicName, String body) {
        return client.publish(qualifiedTopicName, body).join().getMessageId();
    }

    private BatchSubscriptionPolicy singleMessageBatchPolicy() {
        return batchSubscriptionPolicy()
                .withBatchSize(1)
                .withBatchVolume(200)
                .withBatchTime(1)
                .withMessageTtl(100)
                .withMessageBackoff(10)
                .withRequestTimeout(100)
                .build();
    }
}
