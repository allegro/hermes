package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.MetricDecimalValue;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.integrationtests.prometheus.PrometheusExtension;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static java.lang.Integer.MAX_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.api.BatchSubscriptionPolicy.Builder.batchSubscriptionPolicy;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.integrationtests.assertions.HermesAssertions.assertThatMetrics;
import static pl.allegro.tech.hermes.integrationtests.prometheus.SubscriptionMetrics.subscriptionMetrics;
import static pl.allegro.tech.hermes.integrationtests.prometheus.TopicMetrics.topicMetrics;
import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.groupWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class MetricsTest {

    @Order(0)
    @RegisterExtension
    public static final PrometheusExtension prometheus = new PrometheusExtension();

    @Order(1)
    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension()
            .withPrometheus(prometheus);

    @RegisterExtension
    public static final TestSubscribersExtension subscribers = new TestSubscribersExtension();

    @Test
    public void shouldIncreaseTopicMetricsAfterMessageHasBeenPublished() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        hermes.initHelper().createSubscription(
                subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build()
        );
        TestMessage message = TestMessage.simple();
        int attempts = hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            // when
            WebTestClient.ResponseSpec response = hermes.api().getTopicMetrics(topic.getQualifiedName());

            // then
            response.expectStatus().is2xxSuccessful();
            TopicMetrics metrics = response.expectBody(TopicMetrics.class).returnResult().getResponseBody();
            assertThat(metrics).isNotNull();
            assertThat(metrics.getPublished()).isBetween(1L, (long) attempts);
            assertThat(metrics.getVolume()).isGreaterThan(1);
            assertThat(metrics.getSubscriptions()).isEqualTo(1);
        });
    }

    @Test
    public void shouldIncreaseSubscriptionDeliveredMetricsAfterMessageDelivered() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        Subscription subscription = hermes.initHelper().createSubscription(
                subscription(topic.getQualifiedName(), "subscription", subscriber.getEndpoint()).build()
        );
        TestMessage message = TestMessage.simple();
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());
        subscriber.waitUntilReceived(message.body());

        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            // when
            WebTestClient.ResponseSpec response = hermes.api().getSubscriptionMetrics(topic.getQualifiedName(), subscription.getName());

            // then
            response.expectStatus().is2xxSuccessful();
            SubscriptionMetrics metrics = response.expectBody(SubscriptionMetrics.class).returnResult().getResponseBody();
            assertThat(metrics).isNotNull();
            // potentially there were retries, therefore we cannot assume that only one message was delivered
            assertThat(metrics.getDelivered()).isGreaterThan(0);
            assertThat(metrics.getDiscarded()).isEqualTo(0);
            assertThat(metrics.getVolume()).isGreaterThan(1);
        });
    }

    @Test
    public void shouldNotCreateNewSubscriptionWhenAskedForNonExistingMetrics() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        String randomSubscriptionName = UUID.randomUUID().toString();

        // when
        hermes.api().getSubscriptionMetrics(topic.getQualifiedName(), randomSubscriptionName);

        // then
        hermes.api().listSubscriptions(topic.getQualifiedName())
                .expectStatus()
                .isOk()
                .expectBodyList(String.class)
                .doesNotContain(randomSubscriptionName);
    }

    @Test
    public void shouldNotCreateNewTopicWhenAskingForNonExistingMetrics() {
        // given
        Group group = hermes.initHelper().createGroup(groupWithRandomName().build());
        TopicName nonexistentTopicName = new TopicName(group.getGroupName(), "nonexistentTopic");

        // when
        hermes.api().getTopicMetrics(nonexistentTopicName.qualifiedName());

        // then
        hermes.api().listTopics(group.getGroupName())
                .expectStatus()
                .isOk()
                .expectBodyList(String.class)
                .doesNotContain(nonexistentTopicName.qualifiedName());
    }

    @Test
    public void shouldReportHttpErrorCodeMetrics() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber(404);
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        Subscription subscription = hermes.initHelper().createSubscription(
                subscription(topic, "subscription")
                        .withEndpoint(subscriber.getEndpoint())
                        .withSubscriptionPolicy(
                                subscriptionPolicy()
                                        .applyDefaults()
                                        .withMessageTtl(0)
                                        .build()
                        )
                        .build()
        );
        TestMessage message = TestMessage.simple();

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilReceived(message.body());
        hermes.api().getConsumersMetrics()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value((body) -> assertThatMetrics(body)
                        .contains("hermes_consumers_subscription_http_status_codes_total")
                        .withLabels(
                                "group", topic.getName().getGroupName(),
                                "status_code", "404",
                                "subscription", subscription.getName(),
                                "topic", topic.getName().getName()
                        )
                        .withValue(1.0)
                );
    }

    @Test
    public void shouldReportHttpSuccessCodeMetrics() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        Subscription subscription = hermes.initHelper().createSubscription(
                subscription(topic, "subscription")
                        .withEndpoint(subscriber.getEndpoint())
                        .withSubscriptionPolicy(
                                subscriptionPolicy()
                                        .applyDefaults()
                                        .withMessageTtl(0)
                                        .build()
                        )
                        .build()
        );
        TestMessage message = TestMessage.simple();

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        subscriber.waitUntilReceived(message.body());
        hermes.api().getConsumersMetrics()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value((body) -> assertThatMetrics(body)
                        .contains("hermes_consumers_subscription_http_status_codes_total")
                        .withLabels(
                                "group", topic.getName().getGroupName(),
                                "status_code", "200",
                                "subscription", subscription.getName(),
                                "topic", topic.getName().getName()
                        )
                        .withValue(1.0)
                );
    }

    @Test
    public void shouldReportMetricForFilteredSubscription() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        final Subscription subscription = hermes.initHelper().createSubscription(
                subscription(topic, "subscription")
                        .withEndpoint(subscriber.getEndpoint())
                        .withSubscriptionPolicy(
                                subscriptionPolicy()
                                        .applyDefaults()
                                        .withMessageTtl(0)
                                        .build()
                        )
                        .withFilter(filterMatchingHeaderByPattern("Trace-Id", "^vte.*"))
                        .build()
        );
        TestMessage unfiltered = TestMessage.of("msg", "unfiltered");
        TestMessage filtered = TestMessage.of("msg", "filtered");

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), unfiltered.body(), header("Trace-Id", "vte12"));
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), filtered.body(), header("Trace-Id", "otherTraceId"));

        // then
        subscriber.waitUntilReceived(unfiltered.body());
        waitAtMost(Duration.ofSeconds(10)).untilAsserted(() ->
                hermes.api().getConsumersMetrics()
                        .expectStatus()
                        .isOk()
                        .expectBody(String.class)
                        .value((body) -> assertThatMetrics(body)
                                .contains("hermes_consumers_subscription_filtered_out_total")
                                .withLabels(
                                        "group", topic.getName().getGroupName(),
                                        "subscription", subscription.getName(),
                                        "topic", topic.getName().getName()
                                )
                                .withValue(1.0)
                        )
        );
    }

    @Test
    public void shouldReportMetricsForSuccessfulBatchDelivery() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber();
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        final Subscription subscription = hermes.initHelper().createSubscription(
                subscription(topic, "subscription")
                        .withEndpoint(subscriber.getEndpoint())
                        .withSubscriptionPolicy(
                                batchSubscriptionPolicy()
                                        .withBatchSize(2)
                                        .withMessageTtl(MAX_VALUE)
                                        .withRequestTimeout(MAX_VALUE)
                                        .withBatchTime(MAX_VALUE)
                                        .withBatchVolume(1024)
                                        .build()
                        )
                        .build()
        );

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), TestMessage.of("key1", "message").body());
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), TestMessage.of("key2", "message").body());

        // then
        subscriber.waitUntilAnyMessageReceived();
        hermes.api().getConsumersMetrics()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value((body) -> {
                            assertThatMetrics(body)
                                    .contains("hermes_consumers_subscription_delivered_total")
                                    .withLabels(
                                            "group", topic.getName().getGroupName(),
                                            "subscription", subscription.getName(),
                                            "topic", topic.getName().getName()
                                    )
                                    .withValue(2.0);
                            assertThatMetrics(body)
                                    .contains("hermes_consumers_subscription_batches_total")
                                    .withLabels(
                                            "group", topic.getName().getGroupName(),
                                            "subscription", subscription.getName(),
                                            "topic", topic.getName().getName()
                                    )
                                    .withValue(1.0);
                            assertThatMetrics(body)
                                    .contains("hermes_consumers_subscription_http_status_codes_total")
                                    .withLabels(
                                            "group", topic.getName().getGroupName(),
                                            "status_code", "200",
                                            "subscription", subscription.getName(),
                                            "topic", topic.getName().getName()
                                    )
                                    .withValue(1.0);
                        }
                );
    }

    @Test
    public void shouldReportMetricsForFailedBatchDelivery() {
        // given
        TestSubscriber subscriber = subscribers.createSubscriber(404);
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        final Subscription subscription = hermes.initHelper().createSubscription(
                subscription(topic, "subscription")
                        .withEndpoint(subscriber.getEndpoint())
                        .withSubscriptionPolicy(
                                batchSubscriptionPolicy()
                                        .withBatchSize(2)
                                        .withMessageTtl(0)
                                        .withRequestTimeout(MAX_VALUE)
                                        .withBatchTime(MAX_VALUE)
                                        .withBatchVolume(1024)
                                        .build()
                        )
                        .build()
        );

        // when
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), TestMessage.of("key1", "message").body());
        hermes.api().publishUntilSuccess(topic.getQualifiedName(), TestMessage.of("key2", "message").body());

        // then
        subscriber.waitUntilAnyMessageReceived();
        hermes.api().getConsumersMetrics()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value((body) -> assertThatMetrics(body)
                        .contains("hermes_consumers_subscription_http_status_codes_total")
                        .withLabels(
                                "group", topic.getName().getGroupName(),
                                "status_code", "404",
                                "subscription", subscription.getName(),
                                "topic", topic.getName().getName()
                        )
                        .withValue(1.0)
                );
    }

    @Test
    public void shouldReadTopicMetricsFromPrometheus() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        prometheus.stubTopicMetrics(
                topicMetrics(topic.getName())
                        .withRate(10)
                        .withDeliveryRate(15)
                        .build()
        );

        // when
        WebTestClient.ResponseSpec response = hermes.api().getTopicMetrics(topic.getQualifiedName());

        // then
        response.expectStatus().is2xxSuccessful();
        TopicMetrics metrics = response.expectBody(TopicMetrics.class).returnResult().getResponseBody();
        assertThat(metrics).isNotNull();
        assertThat(metrics.getRate()).isEqualTo(MetricDecimalValue.of("10.0"));
        assertThat(metrics.getDeliveryRate()).isEqualTo(MetricDecimalValue.of("15.0"));
    }

    @Test
    public void shouldReadSubscriptionMetricsFromPrometheus() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
        Subscription subscription = hermes.initHelper().createSubscription(
                subscriptionWithRandomName(topic.getName(), "http://endpoint2").build()
        );
        prometheus.stubSubscriptionMetrics(
                subscriptionMetrics(subscription.getQualifiedName())
                        .withRate(15)
                        .build()
        );

        // when
        WebTestClient.ResponseSpec response = hermes.api().getSubscriptionMetrics(topic.getQualifiedName(), subscription.getName());

        // then
        response.expectStatus().is2xxSuccessful();
        SubscriptionMetrics metrics = response.expectBody(SubscriptionMetrics.class).returnResult().getResponseBody();
        assertThat(metrics).isNotNull();
        assertThat(metrics.getRate()).isEqualTo(MetricDecimalValue.of("15.0"));
    }

    private static HttpHeaders header(String key, String value) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(key, value);
        return headers;
    }

    private static MessageFilterSpecification filterMatchingHeaderByPattern(String headerName, String pattern) {
        return new MessageFilterSpecification(Map.of("type", "header", "header", headerName, "matcher", pattern));
    }
}
