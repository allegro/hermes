package pl.allegro.tech.hermes.integration;

import org.glassfish.jersey.client.ClientConfig;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.integration.client.SlowClient;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.metadata.TraceContext;
import pl.allegro.tech.hermes.integration.shame.Unreliable;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.util.UUID;

import static javax.ws.rs.client.ClientBuilder.newClient;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.glassfish.jersey.client.ClientProperties.REQUEST_ENTITY_PROCESSING;
import static org.glassfish.jersey.client.RequestEntityProcessing.CHUNKED;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.integration.helper.ClientBuilderHelper.createRequestWithTraceHeaders;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class PublishingTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldPublishAndConsumeMessage() {
        // given
        Topic topic = operations.buildTopic("publishAndConsumeGroup", "topic");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

        TestMessage message = TestMessage.of("hello", "world");
        remoteService.expectMessages(message.body());

        // when
        Response response = publisher.publish(topic.getQualifiedName(), message.body());

        // then
        assertThat(response).hasStatus(CREATED);
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldMarkSubscriptionAsActiveAfterReceivingFirstMessage() {
        // given
        Topic topic = operations.buildTopic("markAsActiveGroup", "topic");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

        TestMessage message = TestMessage.of("hello", "world");
        remoteService.expectMessages(message.body());

        // when
        Response r = publisher.publish("markAsActiveGroup.topic", message.body());
        assertThat(r).hasStatus(Response.Status.CREATED);

        // then
        remoteService.waitUntilReceived();
        assertThat(management.subscription().get(topic.getQualifiedName(), "subscription").getState()).isEqualTo(Subscription.State.ACTIVE);
    }

    @Test
    public void shouldNotConsumeMessagesWhenSubscriptionIsSuspended() {
        // given
        String subscription = "publishingTestSubscription";

        Topic topic = operations.buildTopic("publishSuspendedGroup", "publishingTestTopic");
        operations.createSubscription(topic, subscription, HTTP_ENDPOINT_URL);
        wait.untilSubscriptionIsActivated(topic, subscription);
        operations.suspendSubscription(topic, subscription);
        wait.untilSubscriptionIsSuspended(topic, subscription);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), TestMessage.of("hello", "world").body());

        // then
        assertThat(response).hasStatus(CREATED);
        remoteService.makeSureNoneReceived();
    }

    @Test
    public void shouldConsumeMessagesOnMultipleSubscriptions() {
        // given
        Topic topic = operations.buildTopic("publishMultipleGroup", "topic");
        operations.createSubscription(topic, "subscription1", HTTP_ENDPOINT_URL + "1/");
        operations.createSubscription(topic, "subscription2", HTTP_ENDPOINT_URL + "2/");

        TestMessage message = TestMessage.of("hello", "world");

        RemoteServiceEndpoint endpoint1 = new RemoteServiceEndpoint(SharedServices.services().serviceMock(), "/1/");
        endpoint1.expectMessages(message.body());
        RemoteServiceEndpoint endpoint2 = new RemoteServiceEndpoint(SharedServices.services().serviceMock(), "/2/");
        endpoint2.expectMessages(message.body());

        // when
        publisher.publish(topic.getQualifiedName(), message.body());

        // then
        endpoint1.waitUntilReceived();
        endpoint2.waitUntilReceived();
    }

    @Test
    public void shouldPublishMessageToEndpointWithInterpolatedURI() {
        // given
        Topic topic = operations.buildTopic("publishInterpolatedGroup", "topic");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL + "{template}/");

        TestMessage message = TestMessage.of("template", "hello");
        RemoteServiceEndpoint interpolatedEndpoint = new RemoteServiceEndpoint(SharedServices.services().serviceMock(), "/hello/");
        interpolatedEndpoint.expectMessages(message.body());

        // when
        publisher.publish(topic.getQualifiedName(), message.body());

        // then
        interpolatedEndpoint.waitUntilReceived();
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldTreatMessageWithInvalidInterpolationAsUndelivered() {
        // given
        Topic topic = operations.buildTopic("publishInvalidInterpolatedGroup", "topic");
        Subscription subscription = subscription(topic, "subscription")
                .withEndpoint(EndpointAddress.of(HTTP_ENDPOINT_URL + "{template}/"))
                .withSubscriptionPolicy(
                        SubscriptionPolicy.Builder.subscriptionPolicy().applyDefaults().withMessageTtl(1).build()
                ).build();
        operations.createSubscription(topic, subscription);

        TestMessage message = TestMessage.of("hello", "world");
        RemoteServiceEndpoint interpolatedEndpoint = new RemoteServiceEndpoint(SharedServices.services().serviceMock(), "/hello/");
        interpolatedEndpoint.expectMessages(message.body());

        // when
        publisher.publish(topic.getQualifiedName(), message.body());

        // then
        interpolatedEndpoint.makeSureNoneReceived();
        wait.untilMessageDiscarded();
        long discarded = management.subscription().getMetrics("publishInvalidInterpolatedGroup.topic", "subscription").getDiscarded();
        assertThat(discarded).isEqualTo(1);
    }

    @Test
    public void shouldNotPublishMessageIfContentLengthDoNotMatch() throws IOException, InterruptedException {
        // given
        Topic topic = operations.buildTopic("invalidContentType", "topic");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

        // when
        publishEventWithInvalidContentLength(topic.getQualifiedName());

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
        Topic topic = operations.buildTopic("chunked", "topic");

        // when
        Response response = newClient(new ClientConfig().property(REQUEST_ENTITY_PROCESSING, CHUNKED))
                .target(FRONTEND_TOPICS_ENDPOINT)
                .path(topic.getQualifiedName()).request().post(Entity.text("{}"));

        // then
        assertThat(response).hasStatus(CREATED);
    }

    @Test
    public void shouldNotCreateTopicWhenPublishingToNonExistingTopic() throws Exception {
        // given
        TopicName nonExisting = TopicName.fromQualifiedName("nonExistingGroup.nonExistingTopic8326");

        // when
        Response responseForNonExisting = publisher.publish(nonExisting.qualifiedName(), TestMessage.simple().body());

        // then
        assertThat(responseForNonExisting.getStatus()).isEqualTo(404);

        ZookeeperPaths paths = new ZookeeperPaths(Configs.ZOOKEEPER_ROOT.getDefaultValue().toString());
        assertThat(SharedServices.services().zookeeper().checkExists().forPath(paths.topicPath(nonExisting))).isNull();
    }

    @Test
    public void shouldPublishAndConsumeMessageWithTraceId() {

        // given
        String message = "{\"id\": 101}";
        String traceId = UUID.randomUUID().toString();

        // and
        Topic topic = operations.buildTopic("traceSendAndReceiveGroup", "topic");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        remoteService.expectMessages(message);
        WebTarget client = ClientBuilder.newClient().target(FRONTEND_URL).path("topics").path(topic.getQualifiedName());

        // when
        Response response = client
                .request()
                .header("Trace-Id", traceId)
                .post(Entity.entity(message, MediaType.APPLICATION_JSON));

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        assertThat(remoteService.waitAndGetLastRequest()).hasHeaderValue("Trace-Id", traceId);
    }

    @Test
    public void shouldPublishAndConsumeMessageWithTraceAndSpanHeaders() {

        // given
        String message = "{\"id\": 101}";
        TraceContext trace = TraceContext.random();

        // and
        Topic topic = operations.buildTopic("traceSendAndReceiveGroup", "topic2");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        remoteService.expectMessages(message);
        Invocation.Builder request = createRequestWithTraceHeaders(FRONTEND_URL, topic.getQualifiedName(), trace);

        // when
        Response response = request.post(Entity.entity(message, MediaType.APPLICATION_JSON));

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        assertThat(remoteService.waitAndGetLastRequest()).containsAllHeaders(trace.asMap());
    }

    @Test
    public void shouldRetryWithDelayOnRetryAfterEndpointResponse() {
        // given
        int retryAfterSeconds = 1;
        String message = "hello";
        remoteService.retryMessage(message, retryAfterSeconds);

        // and
        Topic topic = operations.buildTopic("retryAfterTopic", "topic");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

        // when
        publisher.publish(topic.getQualifiedName(), message);

        // then
        remoteService.waitUntilReceived();

        assertThat(remoteService.durationBetweenFirstAndLastRequest().minusSeconds(retryAfterSeconds).isNegative()).isFalse();
        assertThat(remoteService.receivedMessageWithHeader("Hermes-Retry-Count", "0")).isTrue();
        assertThat(remoteService.receivedMessageWithHeader("Hermes-Retry-Count", "1")).isTrue();
    }

    @Test
    public void shouldPassSubscriptionHeaders() {
        // given
        String message = "abcd";
        Topic topic = operations.buildTopic("headersTestGroup", "topic");

        Subscription subscription = SubscriptionBuilder.subscription(topic, "subscription")
                .withEndpoint(HTTP_ENDPOINT_URL)
                .withHeader("MY-HEADER", "myHeader123")
                .build();
        operations.createSubscription(topic, subscription);
        remoteService.expectMessages(message);
        WebTarget client = ClientBuilder.newClient().target(FRONTEND_URL).path("topics").path(topic.getQualifiedName());

        // when
        Response response = client
                .request()
                .post(Entity.entity(message, MediaType.TEXT_PLAIN));

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        assertThat(remoteService.waitAndGetLastRequest()).hasHeaderValue("MY-HEADER", "myHeader123");
    }

    @Test
    public void shouldNotOverrideHeadersAddedByMetadataAppendersWithSubscriptionHeaders() {
        // given
        String message = "abcd";
        Topic topic = operations.buildTopic("headersAndTracesTestGroup", "topic");

        Subscription subscription = SubscriptionBuilder.subscription(topic, "subscription")
                .withEndpoint(HTTP_ENDPOINT_URL)
                .withHeader("Trace-Id", "defaultValue")
                .build();
        operations.createSubscription(topic, subscription);
        remoteService.expectMessages(message);
        WebTarget client = ClientBuilder.newClient().target(FRONTEND_URL).path("topics").path(topic.getQualifiedName());

        // when
        Response response = client
                .request()
                .header("Trace-Id", "valueFromRequest")
                .post(Entity.entity(message, MediaType.TEXT_PLAIN));

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        assertThat(remoteService.waitAndGetLastRequest()).hasHeaderValue("Trace-Id", "valueFromRequest");
    }
}
