package pl.allegro.tech.hermes.integration;

import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMode;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.consumers.config.ZookeeperProperties;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.integration.client.SlowClient;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.metadata.TraceContext;
import pl.allegro.tech.hermes.integration.shame.Unreliable;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.UUID;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.client.ClientBuilder.newClient;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.glassfish.jersey.client.ClientProperties.REQUEST_ENTITY_PROCESSING;
import static org.glassfish.jersey.client.RequestEntityProcessing.CHUNKED;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.integration.helper.ClientBuilderHelper.createRequestWithTraceHeaders;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class PublishingTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldPublishAndConsumeMessage() {
        // given
        Topic topic = operations.buildTopic(randomTopic("publishAndConsumeGroup", "topic").build());
        operations.createSubscription(topic, "subscription", remoteService.getUrl());

        TestMessage message = TestMessage.of("hello", "world");
        remoteService.expectMessages(message.body());

        // when
        Response response = publisher.publish(topic.getQualifiedName(), message.body());

        // then
        assertThat(response).hasStatus(CREATED);
        remoteService.waitUntilReceived();
    }

    @Test
    public void shouldReturn429ForQuotaViolation() {
        // given
        Topic topic = operations.buildTopic(randomTopic("publishAndConsumeGroup", "topic").build());

        // Frontend is configured in integration test suite to block publisher after 50_000 kb/sec
        TestMessage message = TestMessage.of("content", StringUtils.repeat("X", 60_000));

        wait.until(() -> {
            // when
            Response response = publisher.publish(topic.getQualifiedName(), message.body());

            // then
            assertThat(response.getStatus()).isEqualTo(429);
        });
    }

    @Test
    public void shouldReturn4xxForTooLargeContent() {
        // given
        Topic topic = operations.buildTopic(randomTopic("largeContent", "topic").withMaxMessageSize(2048).build());

        // when
        Response response = publisher.publish(topic.getQualifiedName(), StringUtils.repeat("X", 2555));

        // then
        assertThat(response).hasStatus(BAD_REQUEST);
    }

    @Test
    public void shouldMarkSubscriptionAsActiveAfterReceivingFirstMessage() {
        // given
        Topic topic = operations.buildTopic(randomTopic("markAsActiveGroup", "topic").build());
        operations.createSubscription(topic, "subscription", remoteService.getUrl());

        TestMessage message = TestMessage.of("hello", "world");
        remoteService.expectMessages(message.body());

        // when
        Response r = publisher.publish(topic.getQualifiedName(), message.body());
        assertThat(r).hasStatus(Response.Status.CREATED);

        // then
        remoteService.waitUntilReceived();
        assertThat(management.subscription().get(topic.getQualifiedName(), "subscription").getState()).isEqualTo(Subscription.State.ACTIVE);
    }

    @Test
    public void shouldNotConsumeMessagesWhenSubscriptionIsSuspended() {
        // given
        String subscription = "publishingTestSubscription";

        Topic topic = operations.buildTopic(randomTopic("publishSuspendedGroup", "publishingTestTopic").build());
        operations.createSubscription(topic, subscription, remoteService.getUrl());
        wait.untilSubscriptionIsActivated(topic, subscription);
        operations.suspendSubscription(topic, subscription);
        wait.untilSubscriptionIsSuspended(topic, subscription);
        remoteService.reset();

        // when
        Response response = publisher.publish(topic.getQualifiedName(), TestMessage.of("hello", "world").body());

        // then
        assertThat(response).hasStatus(CREATED);
        remoteService.makeSureNoneReceived();
    }

    @Test
    public void shouldConsumeMessagesOnMultipleSubscriptions() {
        // given
        TestMessage message = TestMessage.of("hello", "world");

        Topic topic = operations.buildTopic(randomTopic("publishMultipleGroup", "topic").build());

        RemoteServiceEndpoint endpoint1 = new RemoteServiceEndpoint(SharedServices.services().serviceMock(), "/1/");
        RemoteServiceEndpoint endpoint2 = new RemoteServiceEndpoint(SharedServices.services().serviceMock(), "/2/");

        operations.createSubscription(topic, "subscription1", endpoint1.getUrl());
        operations.createSubscription(topic, "subscription2", endpoint2.getUrl());

        endpoint1.expectMessages(message.body());
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
        Topic topic = operations.buildTopic(randomTopic("publishInterpolatedGroup", "topic").build());

        TestMessage message = TestMessage.of("template", "hello");
        RemoteServiceEndpoint interpolatedEndpoint = new RemoteServiceEndpoint(SharedServices.services().serviceMock(), "/hello/");
        operations.createSubscription(topic, "subscription", "http://localhost:" + interpolatedEndpoint.getServicePort() + "/{template}/");
        interpolatedEndpoint.expectMessages(message.body());

        // when
        publisher.publish(topic.getQualifiedName(), message.body());

        // then
        interpolatedEndpoint.waitUntilReceived();
    }

    @Unreliable
    @Test
    public void shouldTreatMessageWithInvalidInterpolationAsUndelivered() {
        // given
        Topic topic = operations.buildTopic(randomTopic("publishInvalidInterpolatedGroup", "topic").build());
        RemoteServiceEndpoint interpolatedEndpoint = new RemoteServiceEndpoint(SharedServices.services().serviceMock(), "/hello/");
        Subscription subscription = subscription(topic, "subscription")
                .withEndpoint(EndpointAddress.of(interpolatedEndpoint.getUrl().toString() + "{template}/"))
                .withSubscriptionPolicy(
                        SubscriptionPolicy.Builder.subscriptionPolicy().applyDefaults().withMessageTtl(1).build()
                ).build();
        operations.createSubscription(topic, subscription);

        TestMessage message = TestMessage.of("hello", "world");

        // when
        assertThat(publisher.publish(topic.getQualifiedName(), message.body())).hasStatus(CREATED);

        // then
        wait.until(() -> {
            long discarded = management.subscription().getMetrics(topic.getQualifiedName(), "subscription").getDiscarded();
            assertThat(discarded).isEqualTo(1);
        });
        interpolatedEndpoint.makeSureNoneReceived();
    }

    @Test
    public void shouldNotPublishMessageIfContentLengthDoNotMatch() throws IOException, InterruptedException {
        // given
        Topic topic = operations.buildTopic(randomTopic("invalidContentType", "topic").build());
        operations.createSubscription(topic, "subscription", remoteService.getUrl());

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
    public void shouldPublishMessageUsingChunkedEncoding() {
        // given
        Topic topic = operations.buildTopic(randomTopic("chunked", "topic").build());

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
        ZookeeperProperties zookeeperProperties = new ZookeeperProperties();
        TopicName nonExisting = TopicName.fromQualifiedName("nonExistingGroup.nonExistingTopic8326");

        // when
        Response responseForNonExisting = publisher.publish(nonExisting.qualifiedName(), TestMessage.simple().body());

        // then
        assertThat(responseForNonExisting.getStatus()).isEqualTo(404);

        ZookeeperPaths paths = new ZookeeperPaths(zookeeperProperties.getRoot());
        assertThat(SharedServices.services().zookeeper().checkExists().forPath(paths.topicPath(nonExisting))).isNull();
    }

    @Test
    public void shouldPublishAndConsumeMessageWithTraceId() {

        // given
        String message = "{\"id\": 101}";
        String traceId = UUID.randomUUID().toString();

        // and
        Topic topic = operations.buildTopic(randomTopic("traceSendAndReceiveGroup", "topic").build());
        operations.createSubscription(topic, "subscription", remoteService.getUrl());
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
        Topic topic = operations.buildTopic(randomTopic("traceSendAndReceiveGroup", "topic2").build());
        operations.createSubscription(topic, "subscription", remoteService.getUrl());
        remoteService.expectMessages(message);
        Invocation.Builder request = createRequestWithTraceHeaders(FRONTEND_URL, topic.getQualifiedName(), trace);

        // when
        Response response = request.post(Entity.entity(message, MediaType.APPLICATION_JSON));

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
        assertThat(remoteService.waitAndGetLastRequest()).containsAllHeaders(trace.asMap());
    }

    @Test
    public void shouldConsumeMessageWithKeepAliveHeader() {
        // given
        Topic topic = operations.buildTopic("group", "topic");
        operations.createSubscription(topic, "subscription", remoteService.getUrl());

        TestMessage message = TestMessage.of("hello", "world");

        // when
        Response response = publisher.publish(topic.getQualifiedName(), message.body());

        // then
        assertThat(response).hasStatus(CREATED);

        remoteService.expectMessages(message.body());
        assertThat(remoteService.waitAndGetLastRequest()).hasHeaderValue("Keep-Alive", "true");
    }

    @Test
    public void shouldRetryWithDelayOnRetryAfterEndpointResponse() {
        // given
        int retryAfterSeconds = 1;
        String message = "hello";
        remoteService.retryMessage(message, retryAfterSeconds);

        // and
        Topic topic = operations.buildTopic(randomTopic("retryAfterTopic", "topic").build());
        operations.createSubscription(topic, "subscription", remoteService.getUrl());

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
        Topic topic = operations.buildTopic(randomTopic("headersTestGroup", "topic").build());

        Subscription subscription = SubscriptionBuilder.subscription(topic, "subscription")
                .withEndpoint(remoteService.getUrl())
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
        remoteService.waitUntilRequestReceived(request -> {
            assertThat(request).hasHeaderValue("MY-HEADER", "myHeader123");
            assertThat(request.getHeader("Hermes-Message-Id")).isNotEmpty();
        });
    }

    @Test
    public void shouldNotOverrideHeadersAddedByMetadataAppendersWithSubscriptionHeaders() {
        // given
        String message = "abcd";
        Topic topic = operations.buildTopic(randomTopic("headersAndTracesTestGroup", "topic").build());

        Subscription subscription = SubscriptionBuilder.subscription(topic, "subscription")
                .withEndpoint(remoteService.getUrl())
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

    @Test
    public void shouldPublishWithDelayAndConsumeMessage() {
        // given
        int delay = 2000;
        Topic topic = operations.buildTopic(randomTopic("publishWithDelay", "topic").build());
        Subscription subscriptionWithDelay = subscription(topic, "subscription")
                .withEndpoint(remoteService.getUrl())
                .withContentType(ContentType.JSON)
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults()
                        .withSendingDelay(delay)
                        .build())
                .withMode(SubscriptionMode.ANYCAST)
                .build();
        operations.createSubscription(topic, subscriptionWithDelay);

        TestMessage message = TestMessage.of("hello", "world");
        remoteService.expectMessages(message.body());

        // when
        long publishedTime = System.currentTimeMillis();
        Response response = publisher.publish(topic.getQualifiedName(), message.body());

        // then
        assertThat(response).hasStatus(CREATED);
        remoteService.waitUntilReceived();
        long receivedTime = System.currentTimeMillis();
        assertThat(receivedTime - publishedTime).isGreaterThanOrEqualTo(delay);
    }

    @Test
    public void shouldAttachSubscriptionIdentityHeadersWhenItIsEnabled() {
        // given
        String message = "abcd";
        Topic topic = operations.buildTopic(randomTopic("deliverWithSubscriptionIdentityHeaders", "topic").build());
        Subscription subscription = SubscriptionBuilder.subscription(topic, "subscription")
                .withEndpoint(remoteService.getUrl())
                .withAttachingIdentityHeadersEnabled(true)
                .build();
        operations.createSubscription(topic, subscription);
        remoteService.expectMessages(message);

        // when
        publisher.publish(topic.getQualifiedName(), message);

        // then
        remoteService.waitUntilRequestReceived(request -> {
            assertThat(request.getHeader("Hermes-Topic-Name")).isEqualTo(topic.getQualifiedName());
            assertThat(request.getHeader("Hermes-Subscription-Name")).isEqualTo("subscription");
        });
    }

    @Test
    public void shouldNotAttachSubscriptionIdentityHeadersWhenItIsDisabled() {
        // given
        String message = "abcd";
        Topic topic = operations.buildTopic(randomTopic("deliverWithoutSubscriptionIdentityHeaders", "topic").build());
        Subscription subscription = SubscriptionBuilder.subscription(topic, "subscription")
                .withEndpoint(remoteService.getUrl())
                .withAttachingIdentityHeadersEnabled(false)
                .build();
        operations.createSubscription(topic, subscription);
        remoteService.expectMessages(message);

        // when
        publisher.publish(topic.getQualifiedName(), message);

        // then
        remoteService.waitUntilRequestReceived(request -> {
            assertThat(request.getHeader("Hermes-Topic-Name")).isNull();
            assertThat(request.getHeader("Hermes-Subscription-Name")).isNull();
        });
    }
}
