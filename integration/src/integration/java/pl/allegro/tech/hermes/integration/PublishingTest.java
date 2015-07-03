package pl.allegro.tech.hermes.integration;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.integration.client.SlowClient;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.helper.Assertions;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.test.helper.message.TestMessageSet;
import pl.allegro.tech.hermes.integration.shame.Unreliable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;

import static javax.ws.rs.client.ClientBuilder.newClient;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.glassfish.jersey.client.ClientProperties.REQUEST_ENTITY_PROCESSING;
import static org.glassfish.jersey.client.RequestEntityProcessing.CHUNKED;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class PublishingTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    private Assertions assertions;

    private String schema;

    @BeforeClass
    public void initialize() throws IOException {
        this.assertions = new Assertions(SharedServices.services().zookeeper());

        schema = IOUtils.toString(this.getClass().getResourceAsStream("/schema/example.json"));
    }

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldPublishAndConsumeMessage() {
        // given
        operations.buildSubscription("publishAndConsumeGroup", "topic", "subscription", HTTP_ENDPOINT_URL);

        TestMessage message = TestMessage.of("hello", "world");
        remoteService.expectMessages(message.body());

        // when
        Response response = publisher.publish("publishAndConsumeGroup.topic", message.body());

        // then
        assertThat(response).hasStatus(CREATED);
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldMarkSubscriptionAsActiveAfterReceivingFirstMessage() {
        // given
        operations.buildSubscription("markAsActiveGroup", "topic", "subscription", HTTP_ENDPOINT_URL);

        TestMessage message = TestMessage.of("hello", "world");
        remoteService.expectMessages(message.body());

        // when
        publisher.publish("markAsActiveGroup.topic", message.body());

        // then
        remoteService.waitUntilReceived();
        assertThat(management.subscription().get("markAsActiveGroup.topic", "subscription").getState()).isEqualTo(Subscription.State.ACTIVE);
    }

    @Test
    public void shouldPublishAndConsumeMessageSet() {
        // given
        operations.buildSubscription("publishMessageSetGroup", "topic", "subscription", HTTP_ENDPOINT_URL);

        TestMessageSet messages = TestMessageSet.of(TestMessage.of("hello", "world"), TestMessage.of("hello1", "world"));
        remoteService.expectMessages(TestMessage.of("hello", "world").body(), TestMessage.of("hello1", "world").body());

        // when
        publisher.publish("publishMessageSetGroup.topic", messages.body());

        // then
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldNotConsumeMessagesWhenSubscriptionIsSuspended() throws InterruptedException {
        // given
        String group = "publishSuspendedGroup";
        String topic = "publishingTestTopic";
        String subscription = "publishingTestSubscription";

        operations.buildSubscription(group, topic, subscription, HTTP_ENDPOINT_URL);
        operations.suspendSubscription(group, topic, subscription);
        wait.untilSubscriptionIsDeactivated(group, topic, subscription);
        
        // when
        Response response = publisher.publish(group + "." + topic, TestMessage.of("hello", "world").body());

        // then
        assertThat(response).hasStatus(CREATED);
        remoteService.makeSureNoneReceived();
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldSendPendingMessagesAfterSubscriptionIsResumed() {
        // given
        operations.buildSubscription("publishResumedGroup", "topic", "subscription", HTTP_ENDPOINT_URL);
        operations.suspendSubscription("publishResumedGroup", "topic", "subscription");
        wait.untilSubscriptionIsDeactivated("publishResumedGroup", "topic", "subscription");
        remoteService.expectMessages(TestMessage.of("hello", "world").body());

        // when
        publisher.publish("publishResumedGroup.topic", TestMessage.of("hello", "world").body());

        operations.activateSubscription("publishResumedGroup", "topic", "subscription");

        // then
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldConsumeMessagesOnMultipleSubscriptions() {
        // given
        operations.buildSubscription("publishMultipleGroup", "topic", "subscription1", HTTP_ENDPOINT_URL + "1/");
        operations.createSubscription("publishMultipleGroup", "topic", "subscription2", HTTP_ENDPOINT_URL + "2/");

        TestMessage message = TestMessage.of("hello", "world");

        RemoteServiceEndpoint endpoint1 = new RemoteServiceEndpoint(SharedServices.services().serviceMock(), "/1/");
        endpoint1.expectMessages(message.body());
        RemoteServiceEndpoint endpoint2 = new RemoteServiceEndpoint(SharedServices.services().serviceMock(), "/2/");
        endpoint2.expectMessages(message.body());

        // when
        publisher.publish("publishMultipleGroup.topic", message.body());

        // then
        endpoint1.waitUntilReceived();
        endpoint2.waitUntilReceived();
    }

    @Test
    public void shouldPublishMessageToEndpointWithInterpolatedURI() {
        // given
        operations.buildSubscription("publishInterpolatedGroup", "topic", "subscription", HTTP_ENDPOINT_URL + "{template}/");

        TestMessage message = TestMessage.of("template", "hello");
        RemoteServiceEndpoint interpolatedEndpoint = new RemoteServiceEndpoint(SharedServices.services().serviceMock(), "/hello/");
        interpolatedEndpoint.expectMessages(message.body());

        // when
        publisher.publish("publishInterpolatedGroup.topic", message.body());

        // then
        interpolatedEndpoint.waitUntilReceived();
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldTreatMessageWithInvalidInterpolationAsUndelivered() throws Exception {
        // given
        Subscription subscription = Subscription.Builder.subscription().applyDefaults().withName("subscription").withEndpoint(
                EndpointAddress.of(HTTP_ENDPOINT_URL + "{template}/")
        ).withSubscriptionPolicy(
                SubscriptionPolicy.Builder.subscriptionPolicy().applyDefaults().withMessageTtl(1).build()
        ).build();
        operations.buildTopic("publishInvalidInterpolatedGroup", "topic");
        operations.createSubscription("publishInvalidInterpolatedGroup", "topic", subscription);

        TestMessage message = TestMessage.of("hello", "world");
        RemoteServiceEndpoint interpolatedEndpoint = new RemoteServiceEndpoint(SharedServices.services().serviceMock(), "/hello/");
        interpolatedEndpoint.expectMessages(message.body());

        // when
        publisher.publish("publishInvalidInterpolatedGroup.topic", message.body());

        // then
        interpolatedEndpoint.makeSureNoneReceived();
        wait.untilMessageDiscarded();
        long discarded = management.subscription().getMetrics("publishInvalidInterpolatedGroup.topic", "subscription").getDiscarded();
        assertThat(discarded).isEqualTo(1);
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldWrapMessageWithMetadata() throws IOException {
        // given
        operations.buildSubscription("publishWrapMessageWithMetadataGroup", "topic", "subscription", HTTP_ENDPOINT_URL);

        TestMessage message = TestMessage.of("hello", "world");
        remoteService.expectMessages(message.body());

        // when
        Response response = publisher.publish("publishWrapMessageWithMetadataGroup.topic", message.body());

        // then
        assertThat(response).hasStatus(CREATED);
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldNotPublishMessageIfContentLengthDoNotMatch() throws IOException, InterruptedException {
        // given
        operations.buildSubscription("invalidContentType", "topic", "subscription", HTTP_ENDPOINT_URL);

        // when
        publishEventWithInvalidContentLength("invalidContentType.topic");

        // then
        remoteService.makeSureNoneReceived();
    }

    /*
        The only way we managed to bypass in-built server side Content-Length validation
        is to close the connection before entire message is delivered. Unfortunately we
        cannot verify response status code because connection is already closed. We tried
        manually sending different Content-Length than the actual message but servlet
        container is smart enough to not invoke ReadListener.onAllDataRead() in that case.
    */
    @SuppressWarnings("PMD.EmptyCatchBlock")
    private void publishEventWithInvalidContentLength(String topic) throws IOException, InterruptedException {
        try {
            new SlowClient().slowEvent(500, 100, 0, topic);
        } catch (SocketTimeoutException e) {
            // this is expected
        }
    }

    @Test
    public void shouldPublishMessageUsingChunkedEncoding() throws UnsupportedEncodingException {
        // given
        operations.buildTopic("chunked", "topic");

        // when
        Response response = newClient(new ClientConfig().property(REQUEST_ENTITY_PROCESSING, CHUNKED))
                .target(FRONTEND_TOPICS_ENDPOINT)
                .path("chunked.topic").request().post(Entity.text("{}"));

        // then
        assertThat(response).hasStatus(CREATED);
    }

    @Test
    public void shouldNotCreateTopicWhenPublishingToNonExistingTopic() throws Exception {
        TopicName nonExisting = TopicName.fromQualifiedName("nonExistingGroup.nonExistingTopic8326");
        TopicName existing = TopicName.fromQualifiedName("existingGroup.topic");
        operations.buildTopic(existing.getGroupName(), existing.getName());

        Response responseForNonExisting = publisher.publish(nonExisting.qualifiedName(), TestMessage.simple().body());
        Response responseForExisting = publisher.publish(existing.qualifiedName(), TestMessage.simple().body());

        assertThat(responseForNonExisting.getStatus()).isEqualTo(404);
        assertThat(responseForExisting.getStatus()).isEqualTo(201);

        wait.untilTopicDetailsAreCreated(existing);
        assertions.topicDetailsNotExists(nonExisting);
    }

    @Test
    public void shouldPublishValidMessageWithJsonSchema() {
        //given
        String message = "{\"id\": 6}";
        operations.buildTopic(topic().withName("schema.topic").withValidation(true).withMessageSchema(schema).build());

        //when
        Response response = publisher.publish("schema.topic", message);

        //then
        assertThat(response).hasStatus(CREATED);
    }

    @Test
    public void shouldNotPublishInvalidMessageWithJsonSchema() {
        // given
        String messageInvalidWithSchema = "{\"id\": \"shouldBeNumber\"}";
        operations.buildTopic(topic().withName("schema.topic").withValidation(true).withMessageSchema(schema).build());

        //when
        Response response = publisher.publish("schema.topic", messageInvalidWithSchema);

        //then
        assertThat(response).hasStatus(BAD_REQUEST);
    }

}
