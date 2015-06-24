package pl.allegro.tech.hermes.integration;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.client.HermesClient;
import pl.allegro.tech.hermes.client.jersey.JerseyHermesSender;
import pl.allegro.tech.hermes.common.message.tracker.LogSchemaAware;
import pl.allegro.tech.hermes.integration.env.FongoFactory;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.integration.test.HermesAssertions;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static javax.ws.rs.client.ClientBuilder.newClient;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient;

public class MessageTrackingTest extends IntegrationTest {

    public static final String INVALID_ENDPOINT_URL = "http://localhost:60000";
    public static final TestMessage MESSAGE = TestMessage.of("hello", "world");

    private DBCollection publishedMessages;
    private DBCollection sentMessages;
    private RemoteServiceEndpoint remoteService;
    private HermesClient client;

    @BeforeClass
    public void initialize() {
        DB mongo = FongoFactory.getInstance().getDB("hermesMessages");
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

    @Test
    public void shouldLogMessagePublishing() {
        // given
        operations.buildTopic(topic().withName("logMessagePublishing", "topic").withTrackingEnabled(true).build());
        
        // when
        publisher.publish("logMessagePublishing.topic", MESSAGE.body());

        // then
        wait.untilMessageTraceLogged(publishedMessages, PublishedMessageTraceStatus.SUCCESS);
        assertThat(findAllStatusesByTopic(publishedMessages, "logMessagePublishing.topic")).contains("SUCCESS");
    }

    @Test
    public void shouldLogMessageInflightAndSending() {
        // given
        operations.buildTopic("logMessageSending", "topic");
        Subscription subscription = subscription().applyDefaults().withName("subscription")
                .withEndpoint(EndpointAddress.of(HTTP_ENDPOINT_URL))
                .withTrackingEnabled(true)
                .build();

        operations.createSubscription("logMessageSending", "topic", subscription);
        remoteService.expectMessages(MESSAGE.body());

        // when
        publisher.publish("logMessageSending.topic", MESSAGE.body());
        wait.untilReceivedAnyMessage(sentMessages);

        // then
        wait.untilMessageTraceLogged(sentMessages, SentMessageTraceStatus.SUCCESS);

        assertThat(findAllStatusesByTopic(sentMessages, "logMessageSending.topic")).contains("INFLIGHT", "SUCCESS");
    }

    @Test
    public void shouldLogMessageDiscarding() {
        // given
        operations.buildTopic("logMessageDiscarding", "topic");
        Subscription subscription = subscription().withName("subscription")
                .withEndpoint(EndpointAddress.of(INVALID_ENDPOINT_URL))
                .withTrackingEnabled(true)
                .withSubscriptionPolicy(subscriptionPolicy().withRate(1).withMessageTtl(3).build())
                .build();

        operations.createSubscription("logMessageDiscarding", "topic", subscription);

        // when
        publisher.publish("logMessageDiscarding.topic", MESSAGE.body());

        // then
        wait.untilMessageTraceLogged(sentMessages, SentMessageTraceStatus.FAILED);
        wait.untilMessageTraceLogged(sentMessages, SentMessageTraceStatus.DISCARDED);
    }

    @Test
    public void shouldFetchUndeliveredMessagesLogs() {
        // given
        operations.createGroup("fetchUndeliveredMessagesLogs");
        operations.createTopic(Topic.Builder.topic().withName("fetchUndeliveredMessagesLogs", "topic").withTrackingEnabled(true).build());

        Subscription subscription = subscription().withName("subscription")
                .withEndpoint(EndpointAddress.of(INVALID_ENDPOINT_URL))
                .withTrackingEnabled(true)
                .withSubscriptionPolicy(subscriptionPolicy().withRate(1).withMessageTtl(5).build())
                .build();

        operations.createSubscription("fetchUndeliveredMessagesLogs", "topic", subscription);

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

    @Test
    public void shouldToggleTrackingOnTopicUpdate() {
        // given
        TopicName topicName = new TopicName("toggleTrackingOnTopic", "topic");
        Topic topic = topic().withName(topicName).withTrackingEnabled(true).build();
        operations.buildTopic(topic);

        String firstTracked = publishMessage("toggleTrackingOnTopic.topic", MESSAGE.body());
        wait.untilMessageIdLogged(publishedMessages, firstTracked);

        // when
        operations.updateTopic(topicName, topic().applyPatch(topic).withTrackingEnabled(false).build());
        wait.untilTopicUpdated();

        publishMessage("toggleTrackingOnTopic.topic", MESSAGE.body());

        operations.updateTopic(topicName, topic().applyPatch(topic).withTrackingEnabled(true).build());
        wait.untilTopicUpdated();

        String secondTracked = publishMessage("toggleTrackingOnTopic.topic", MESSAGE.body());
        wait.untilMessageIdLogged(publishedMessages, secondTracked);

        // then
        assertThat(findAllMessageIdsByTopic(publishedMessages, "toggleTrackingOnTopic.topic")).containsOnly(firstTracked, secondTracked);
    }

    @Test
    public void shouldNotChangeAckWhenEnablingTrackingOnTopic() {
        // given
        TopicName topicName = new TopicName("ackStaysOnTracking", "topic");
        Topic topic = topic().withName(topicName).withAck(Topic.Ack.ALL).withTrackingEnabled(false).build();
        operations.buildTopic(topic);

        // when
        operations.updateTopic(topicName, topic().withTrackingEnabled(true).build());
        wait.untilTopicUpdated();

        Topic updatedTopic = operations.getTopic("ackStaysOnTracking", "topic");

        // then
        assertThat(updatedTopic.isTrackingEnabled()).isTrue();
        assertThat(updatedTopic.getAck()).isEqualTo(Topic.Ack.ALL);
    }


    @Test
    public void shouldToggleTrackingOnSubscriptionUpdate() {
        // given
        operations.buildTopic("toggleTrackingOnSubscription", "topic");
        Subscription subscription = subscription().applyDefaults().withName("subscription")
                .withEndpoint(EndpointAddress.of(HTTP_ENDPOINT_URL))
                .withTrackingEnabled(true)
                .build();

        operations.createSubscription("toggleTrackingOnSubscription", "topic", subscription);
        remoteService.expectMessages(MESSAGE.body(), MESSAGE.body());
        String firstTracked = publishMessage("toggleTrackingOnSubscription.topic", MESSAGE.body());
        wait.untilMessageIdLogged(sentMessages, firstTracked);

        // when
        operations.updateSubscription("toggleTrackingOnSubscription", "topic", "subscription",
                subscription().applyPatch(subscription).withTrackingEnabled(false).build());

        wait.untilSubscriptionUpdated();

        publishMessage("toggleTrackingOnSubscription.topic", MESSAGE.body());
        remoteService.waitUntilReceived();

        operations.updateSubscription("toggleTrackingOnSubscription", "topic", "subscription",
                subscription().applyPatch(subscription).withTrackingEnabled(true).build());

        wait.untilSubscriptionUpdated();

        remoteService.expectMessages(MESSAGE.body());
        String secondTracked = publishMessage("toggleTrackingOnSubscription.topic", MESSAGE.body());
        remoteService.waitUntilReceived();

        wait.untilMessageIdLogged(sentMessages, secondTracked);

        // then
        assertThat(findAllMessageIdsByTopic(sentMessages, "toggleTrackingOnSubscription.topic")).containsOnly(firstTracked, secondTracked);
    }

    @Test
    public void shouldReturnEmptyListIfThereAreNoSentMessages() {
        //given
        assertThat(sentMessages.count()).isZero();
        operations.buildSubscription("returnEmptyListIfThereAreNoSentMessages", "topic", "subscription3", INVALID_ENDPOINT_URL);

        //when
        Response response = management.subscription().getLatestUndeliveredMessages("returnEmptyListIfThereAreNoSentMessages.topic",
                "subscription3");

        //then
        assertThat(readSentMessages(response)).isEmpty();
    }

    private List<SentMessageTrace> readSentMessages(Response response) {
        HermesAssertions.assertThat(response).hasStatus(Response.Status.OK);
        return asList(response.readEntity(SentMessageTrace[].class));
    }

    private List<String> findAllMessageIdsByTopic(DBCollection collection, String topicName) {
        return collection.find(new BasicDBObject("topicName", topicName)).toArray()
                .stream().map((dbo) -> dbo.get("messageId").toString()).collect(Collectors.toList());
    }

    private List<String> findAllStatusesByTopic(DBCollection collection, String topicName) {
        return collection.find(new BasicDBObject("topicName", topicName)).toArray()
                .stream().map((dbo) -> dbo.get("status").toString()).collect(Collectors.toList());
    }

    private String publishMessage(String qualifiedTopicName, String body) {
        return client.publish(qualifiedTopicName, body).join().getMessageId();
    }
}
