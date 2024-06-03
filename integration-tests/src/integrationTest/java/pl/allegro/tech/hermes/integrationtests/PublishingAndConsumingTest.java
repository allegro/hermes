package pl.allegro.tech.hermes.integrationtests;

import jakarta.ws.rs.core.Response;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.SubscriptionMode;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.integrationtests.helpers.TraceHeaders;
import pl.allegro.tech.hermes.integrationtests.metadata.TraceContext;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static jakarta.ws.rs.core.Response.Status.CREATED;
import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.integrationtests.assertions.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;
import static pl.allegro.tech.hermes.utils.Headers.createHeaders;

public class PublishingAndConsumingTest {

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    @RegisterExtension
    public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

    @Test
    public void shouldPublishAndConsumeMessage() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(), "subscription1", subscriber.getEndpoint()).build());
        TestMessage message = TestMessage.of("hello", "world");

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilReceived(message.body());
    }

    @Test
    public void shouldConsumeMessagesOnMultipleSubscriptions() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSubscriber subscriber1 = subscribers.createSubscriber();
        TestSubscriber subscriber2 = subscribers.createSubscriber();
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(), "subscription1", subscriber1.getEndpoint()).build());
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(), "subscription2", subscriber2.getEndpoint()).build());

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber1.waitUntilReceived(message.body());
        subscriber2.waitUntilReceived(message.body());
    }

    @Test
    public void shouldPublishMessageToEndpointWithURIInterpolatedFromMessageBody() {
        // given
        TestMessage message = TestMessage.of("template", "hello");
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

        TestSubscriber subscriber = subscribers.createSubscriber("/hello/");
        String interpolatedEndpoint = subscriber.getEndpoint().replace("/hello/", "/{template}/");
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(), "subscription", interpolatedEndpoint).build());

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilReceived(message.body());
    }

    @Test
    public void shouldTreatMessageWithInvalidInterpolationAsUndelivered() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSubscriber subscriber = subscribers.createSubscriber("/hello/");

        String interpolatedEndpoint = subscriber.getEndpoint().replace("/hello/", "/{template}/");
        hermes.initHelper().createSubscription(
                subscription(topic.getQualifiedName(), "subscription", interpolatedEndpoint)
                        .withSubscriptionPolicy(SubscriptionPolicy.Builder.subscriptionPolicy().applyDefaults().withMessageTtl(1).build())
                        .build());

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            long discarded = hermes.api()
                    .getSubscriptionMetrics(topic.getQualifiedName(), "subscription")
                    .expectBody(SubscriptionMetrics.class).returnResult().getResponseBody().getDiscarded();
            assertThat(discarded).isEqualTo(1);
        });
        subscriber.noMessagesReceived();
    }

    @Test
    public void shouldPassSubscriptionFixedHeaders() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSubscriber subscriber = subscribers.createSubscriber();
        Subscription subscription = SubscriptionBuilder.subscriptionWithRandomName(topic.getName())
                .withEndpoint(subscriber.getEndpoint())
                .withHeader("MY-HEADER", "myHeader123")
                .build();
        hermes.initHelper().createSubscription(subscription);

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request).hasHeaderValue("MY-HEADER", "myHeader123");
            assertThat(request.getHeader("Hermes-Message-Id")).isNotEmpty();
        });
    }

    @Test
    public void shouldRetryWithDelayOnRetryAfterEndpointResponse() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestMessage message = TestMessage.of("hello", "world");
        int retryAfterSeconds = 1;
        TestSubscriber subscriber = subscribers.createSubscriberWithRetry(message.body(), retryAfterSeconds);
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build());

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilMessageWithHeaderReceived("Hermes-Retry-Count", "0");
        subscriber.waitUntilMessageWithHeaderReceived("Hermes-Retry-Count", "1");
        assertThat(subscriber.durationBetweenFirstAndLastRequest()).isGreaterThanOrEqualTo(Duration.ofSeconds(retryAfterSeconds));
    }

    @Test
    public void shouldNotPublishMessageIfContentLengthDoNotMatch() throws IOException, InterruptedException {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSubscriber subscriber = subscribers.createSubscriber();

        // when
        publishEventWithInvalidContentLength(topic.getQualifiedName());

        // then
        subscriber.noMessagesReceived();
    }

    @Test
    public void shouldConsumeMessageWithKeepAliveHeader() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSubscriber subscriber = subscribers.createSubscriber();
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(), "subscription1", subscriber.getEndpoint()).build());

        TestMessage message = TestMessage.of("hello", "world");

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request.getBodyAsString()).isEqualTo(message.body());
            assertThat(request).hasHeaderValue("Keep-Alive", "true");
        });
    }

    @Test
    public void shouldMarkSubscriptionAsActiveAfterReceivingFirstMessage() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSubscriber subscriber = subscribers.createSubscriber();
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(), "subscription1", subscriber.getEndpoint()).build());

        TestMessage message = TestMessage.of("hello", "world");

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilReceived(message.body());

        assertThat(hermes.api().getSubscription(topic.getQualifiedName(), "subscription1").getState()).isEqualTo(Subscription.State.ACTIVE);
    }

    @Test
    public void shouldNotConsumeMessagesWhenSubscriptionIsSuspended() {
        // given
        String subscriptionName = "subscription1";
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSubscriber subscriber = subscribers.createSubscriber();

        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(), subscriptionName, subscriber.getEndpoint()).build());

        // when
        hermes.api().waitUntilSubscriptionActivated(topic.getQualifiedName(), subscriptionName);
        hermes.api().suspendSubscription(topic, subscriptionName);
        hermes.api().waitUntilSubscriptionSuspended(topic.getQualifiedName(), subscriptionName);
        hermes.api().publishUntilSuccess(topic.getQualifiedName(),  TestMessage.of("hello", "world").body());

        // then
        subscriber.noMessagesReceived();
    }

    @Test
    public void shouldNotCreateTopicWhenPublishingToNonExistingTopic() {
        // given
        TopicName nonExisting = TopicName.fromQualifiedName("nonExistingGroup.nonExistingTopic8326");

        // when
        WebTestClient.ResponseSpec responseForNonExisting = hermes.api().publish(nonExisting.qualifiedName(), TestMessage.simple().body());

        // then
        responseForNonExisting.expectStatus().isNotFound();
        hermes.api().getTopicResponse(nonExisting.qualifiedName()).expectStatus().isNotFound();
    }

    @Test
    public void shouldNotOverrideHeadersAddedByMetadataAppendersWithSubscriptionHeaders() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        String subscriptionName = "subscription";

        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSubscriber subscriber = subscribers.createSubscriber();

        hermes.initHelper().createSubscription(
                SubscriptionBuilder.subscription(topic, subscriptionName)
                        .withEndpoint(subscriber.getEndpoint())
                        .withHeader("Trace-Id", "defaultValue")
                        .build());

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body(), createHeaders(Map.of("Trace-Id", "valueFromRequest")));

        // then
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request.getBodyAsString()).isEqualTo(message.body());
            assertThat(request.getHeader("Trace-Id")).isEqualTo("valueFromRequest");
        });
    }

    @Test
    public void shouldAttachSubscriptionIdentityHeadersWhenItIsEnabled() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        String subscriptionName = "subscription";

        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSubscriber subscriber = subscribers.createSubscriber();

        hermes.initHelper().createSubscription(
                SubscriptionBuilder.subscription(topic, subscriptionName)
                        .withEndpoint(subscriber.getEndpoint())
                        .withAttachingIdentityHeadersEnabled(true)
                        .build());

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request.getBodyAsString()).isEqualTo(message.body());
            assertThat(request.getHeader("Hermes-Topic-Name")).isEqualTo(topic.getQualifiedName());
            assertThat(request.getHeader("Hermes-Subscription-Name")).isEqualTo(subscriptionName);
        });
    }

    @Test
    public void shouldNotAttachSubscriptionIdentityHeadersWhenItIsDisabled() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSubscriber subscriber = subscribers.createSubscriber();

        hermes.initHelper().createSubscription(
                SubscriptionBuilder.subscription(topic, "subscription")
                        .withEndpoint(subscriber.getEndpoint())
                        .withAttachingIdentityHeadersEnabled(false)
                        .build());

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request.getBodyAsString()).isEqualTo(message.body());
            Assertions.assertThat(request.getHeader("Hermes-Topic-Name")).isNull();
            Assertions.assertThat(request.getHeader("Hermes-Subscription-Name")).isNull();
        });
    }

    @Test
    public void shouldPublishAndConsumeMessageWithTraceId() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        String traceId = UUID.randomUUID().toString();

        // and
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSubscriber subscriber = subscribers.createSubscriber();
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build());

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body(), createHeaders(Map.of("Trace-Id", traceId)));

        // then
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request.getBodyAsString()).isEqualTo(message.body());
            assertThat(request).hasHeaderValue("Trace-Id", traceId);
        });
    }

    @Test
    public void shouldPublishAndConsumeMessageWithTraceAndSpanHeaders() {
        // given
        TestMessage message = TestMessage.of("hello", "world");
        TraceContext trace = TraceContext.random();

        // and
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSubscriber subscriber = subscribers.createSubscriber();
        hermes.initHelper().createSubscription(subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build());

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body(), createHeaders(TraceHeaders.fromTraceContext(trace)));

        // then
        subscriber.waitUntilRequestReceived(request -> {
            assertThat(request.getBodyAsString()).isEqualTo(message.body());
            assertThat(request).containsAllHeaders(trace.asMap());
        });
    }

    @Test
    public void shouldPublishWithDelayAndConsumeMessage() {
        // given
        int delay = 2000;
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        TestSubscriber subscriber = subscribers.createSubscriber();

        hermes.initHelper().createSubscription(
                SubscriptionBuilder.subscription(topic, "subscription")
                .withEndpoint(subscriber.getEndpoint())
                .withContentType(ContentType.JSON)
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults()
                        .withSendingDelay(delay)
                        .build())
                .withMode(SubscriptionMode.ANYCAST)
                .build());


        TestMessage message = TestMessage.of("hello", "world");

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());
        long publishedTime = System.currentTimeMillis();

        // then
        subscriber.waitUntilReceived(message.body());
        long receivedTime = System.currentTimeMillis();
        assertThat(receivedTime - publishedTime).isGreaterThanOrEqualTo(delay);
    }

    @Test
    public void shouldPublishMessageUsingChunkedEncoding() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

        // when
        Response response = hermes.api().publishChunked(topic.getQualifiedName(), "{}");

        // then
        assertThat(response).hasStatus(CREATED);
    }

    /*
        The only way we managed to bypass in-built server side Content-Length validation
        is to close the connection before entire message is delivered. Unfortunately we
        cannot verify response status code because connection is already closed. We tried
        manually sending different Content-Length than the actual message but servlet
        container is smart enough to not invoke ReadListener.onAllDataRead() in that case.
    */
    private void publishEventWithInvalidContentLength(String topic) throws IOException, InterruptedException {
        try {
            hermes.api().publishSlowly(500, 100, 0, topic);
        } catch (SocketTimeoutException e) {
            // this is expected
        }
    }

}
