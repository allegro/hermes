package pl.allegro.tech.hermes.integration;

import com.googlecode.catchexception.CatchException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.helper.GraphiteEndpoint;
import pl.allegro.tech.hermes.integration.helper.graphite.GraphiteMockServer;
import pl.allegro.tech.hermes.integration.shame.Unreliable;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.BadRequestException;
import java.util.UUID;

import static com.google.common.collect.ImmutableMap.of;
import static com.googlecode.catchexception.CatchException.catchException;
import static java.lang.Integer.MAX_VALUE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.integration.helper.GraphiteEndpoint.subscriptionMetricsStub;
import static pl.allegro.tech.hermes.api.BatchSubscriptionPolicy.Builder.batchSubscriptionPolicy;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

public class MetricsTest extends IntegrationTest {

    private GraphiteEndpoint graphiteEndpoint;

    private RemoteServiceEndpoint remoteService;

    private GraphiteMockServer graphiteServer;

    @BeforeMethod
    public void initializeAlways() {
        this.graphiteEndpoint = new GraphiteEndpoint(SharedServices.services().graphiteHttpMock());
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
        this.graphiteServer = SharedServices.services().graphiteMock();
    }

    @Unreliable
    @Test
    public void shouldIncreaseTopicMetricsAfterMessageHasBeenPublished() {
        // given
        Topic topic = operations.buildTopic("pl.group", "topic_metrics");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        graphiteEndpoint.returnMetricForTopic("pl_group", "topic_metrics", 10, 15);

        remoteService.expectMessages(TestMessage.simple().body());
        assertThat(publisher.publish("pl.group.topic_metrics", TestMessage.simple().body()).getStatus())
                .isEqualTo(CREATED.getStatusCode());
        remoteService.waitUntilReceived();

        wait.until(() -> {
            // when
            TopicMetrics metrics = management.topic().getMetrics("pl.group.topic_metrics");

            // then
            assertThat(metrics.getRate()).isEqualTo("10");
            assertThat(metrics.getDeliveryRate()).isEqualTo("15");
            assertThat(metrics.getPublished()).isEqualTo(1);
        });
    }

    @Unreliable
    @Test
    public void shouldIncreaseSubscriptionDeliveredMetricsAfterMessageDelivered() {
        // given
        Topic topic = operations.buildTopic("pl.group", "topic");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        graphiteEndpoint.returnMetric(subscriptionMetricsStub("pl_group.topic.subscription").withRate(15).build());

        remoteService.expectMessages(TestMessage.simple().body());
        assertThat(publisher.publish("pl.group.topic", TestMessage.simple().body()).getStatus())
                .isEqualTo(CREATED.getStatusCode());
        remoteService.waitUntilReceived();

        wait.until(() -> {
            // when
            SubscriptionMetrics metrics = management.subscription().getMetrics("pl.group.topic", "subscription");

            // then
            assertThat(metrics.getRate()).isEqualTo("15");
            assertThat(metrics.getDelivered()).isEqualTo(1);
            assertThat(metrics.getDiscarded()).isEqualTo(0);
            assertThat(metrics.getInflight()).isEqualTo(0);
        });
    }

    @Unreliable
    @Test
    public void shouldNotCreateNewSubscriptionWhenAskedForNonExistingMetrics() {
        // given
        TopicName topic = new TopicName("pl.group.sub.bug", "topic");
        operations.buildTopic(topic.getGroupName(), topic.getName());
        String randomSubscription = UUID.randomUUID().toString();

        // when
        catchException(management.subscription())
                .getMetrics(topic.qualifiedName(), randomSubscription);

        // then
        assertThat(management.subscription().list(topic.qualifiedName(), false)).doesNotContain(randomSubscription);
        assertThat(CatchException.<BadRequestException>caughtException())
                .isInstanceOf(BadRequestException.class);
    }

    @Unreliable
    @Test
    public void shouldReadSubscriptionDeliveryRate() {
        // given
        Topic topic = operations.buildTopic("pl.allegro.tech.hermes", "topic");
        operations.createSubscription(topic, "pl.allegro.tech.hermes.subscription", HTTP_ENDPOINT_URL);
        graphiteEndpoint.returnMetric(subscriptionMetricsStub("pl_allegro_tech_hermes.topic.pl_allegro_tech_hermes_subscription").withRate(15).build());

        wait.until(() -> {
            // when
            SubscriptionMetrics metrics = management.subscription().getMetrics("pl.allegro.tech.hermes.topic",
                    "pl.allegro.tech.hermes.subscription");

            // then
            assertThat(metrics.getRate()).isEqualTo("15");
        });
    }

    @Unreliable
    @Test
    public void shouldSendMetricToGraphite() {
        // given
        operations.buildTopic("metricGroup", "topic");
        graphiteServer.expectMetric(metricNameWithPrefix("producer.*.ack-leader.latency.metricGroup.topic.count"), 1);

        // when
        assertThat(publisher.publish("metricGroup.topic", TestMessage.simple().body()).getStatus()).isEqualTo(CREATED.getStatusCode());

        // then
        graphiteServer.waitUntilReceived();
    }

    @Unreliable
    @Test
    public void shouldNotCreateNewTopicWhenAskingForNonExistingMetrics() {
        // given
        TopicName newTopic = new TopicName("auto-topic-bug", "not-existing");
        operations.createGroup(newTopic.getGroupName());

        // when
        management.topic().getMetrics(newTopic.qualifiedName());

        // then
        assertThat(management.topic().list(newTopic.getGroupName(), false)).doesNotContain(newTopic.qualifiedName());
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldNotReportMetricsToConfigStorageForRemovedSubscription() {
        // given
        Topic topic = operations.buildTopic("metricsAfterSubscriptionRemovedGroup", "topic");
        String subscriptionName1 = "subscription";
        operations.createSubscription(topic, subscriptionName1, HTTP_ENDPOINT_URL);
        remoteService.expectMessages(TestMessage.simple().body());

        assertThat(publisher.publish(topic.getQualifiedName(), TestMessage.simple().body())).isEqualTo(CREATED);
        remoteService.waitUntilReceived();

        wait.untilSubscriptionMetricsIsCreated(topic.getName(), subscriptionName1);

        // when
        management.subscription().remove(topic.getQualifiedName(), subscriptionName1);

        // then
        wait.untilSubscriptionMetricsIsRemoved(topic.getName(), subscriptionName1);

        String subscriptionName2 = "subscription2";
        operations.createSubscription(topic, subscriptionName2, HTTP_ENDPOINT_URL);
        management.topic().publishMessage(topic.getQualifiedName(), TestMessage.simple().body());
        wait.untilSubscriptionMetricsIsCreated(topic.getName(), subscriptionName2);
        wait.untilSubscriptionMetricsIsRemoved(topic.getName(), subscriptionName1);
    }

    @Unreliable
    @Test
    public void shouldReportHttpErrorCodeMetrics() {
        // given
        Topic topic = operations.buildTopic("statusErrorGroup", "topic");
        operations.createSubscription(topic, subscription(topic, "subscription")
                .withEndpoint(HTTP_ENDPOINT_URL)
                .withSubscriptionPolicy(SubscriptionPolicy.Builder.subscriptionPolicy()
                        .applyDefaults()
                        .withMessageTtl(0)
                        .build())
                .build());

        remoteService.setReturnedStatusCode(404);
        graphiteServer.expectMetric(metricNameWithPrefix("consumer.*.status.statusErrorGroup.topic.subscription.4xx.404.count"), 1);
        graphiteServer.expectMetric(metricNameWithPrefix("consumer.*.status.statusErrorGroup.topic.subscription.4xx.count"), 1);
        remoteService.expectMessages(TestMessage.simple().body());

        // when
        assertThat(publisher.publish(topic.getQualifiedName(), TestMessage.simple().body()).getStatus())
                .isEqualTo(CREATED.getStatusCode());

        // then
        graphiteServer.waitUntilReceived();
    }

    @Unreliable
    @Test
    public void shouldReportHttpSuccessCodeMetrics() {
        // given
        Topic topic = operations.buildTopic("statusSuccessGroup", "topic");
        String subscriptionName = "subscription";
        operations.createSubscription(topic, subscriptionName, HTTP_ENDPOINT_URL);
        graphiteServer.expectMetric(metricNameWithPrefix("consumer.*.status.statusSuccessGroup.topic.subscription.2xx.200.count"), 1);
        graphiteServer.expectMetric(metricNameWithPrefix("consumer.*.status.statusSuccessGroup.topic.subscription.2xx.count"), 1);
        remoteService.expectMessages(TestMessage.simple().body());

        // when
        assertThat(publisher.publish(topic.getQualifiedName(), TestMessage.simple().body()).getStatus())
                .isEqualTo(CREATED.getStatusCode());

        // then
        graphiteServer.waitUntilReceived();
    }

    @Test
    public void shouldReportMetricForFilteredSubscription() {
        // given
        Topic topic = operations.buildTopic("filteredGroup", "topic");
        operations.createSubscription(topic, subscription(topic.getName(), "subscription")
                .withEndpoint(HTTP_ENDPOINT_URL)
                .withFilter(filterMatchingHeaderByPattern("Trace-Id", "^vte.*"))
                .build());
        graphiteServer.expectMetric(metricNameWithPrefix("consumer.*.meter.filteredGroup.topic.subscription.filtered.count"), 1);

        // when
        publisher.publish("filteredGroup.topic", jsonWithField("msg", "unfiltered"), of("Trace-Id", "vte12"));
        publisher.publish("filteredGroup.topic", jsonWithField("msg", "filtered"), of("Trace-Id", "otherTraceId"));

        // then
        graphiteServer.waitUntilReceived();
    }

    @Test
    public void shouldReportMetricsForSuccessfulBatchDelivery() {
        // given
        Topic topic = operations.buildTopic("successBatchGroup", "topic");
        operations.createSubscription(topic, subscription(topic, "subscription")
                .withEndpoint(HTTP_ENDPOINT_URL)
                .withSubscriptionPolicy(batchSubscriptionPolicy()
                        .withBatchSize(2)
                        .withMessageTtl(MAX_VALUE)
                        .withRequestTimeout(MAX_VALUE)
                        .withBatchTime(MAX_VALUE)
                        .withBatchVolume(1024)
                        .build())
                .build());

        graphiteServer.expectMetric(metricNameWithPrefix("consumer.*.meter.successBatchGroup.topic.subscription.count"), 2);
        graphiteServer.expectMetric(metricNameWithPrefix("consumer.*.meter.successBatchGroup.topic.subscription.batch.count"), 1);
        graphiteServer.expectMetric(metricNameWithPrefix("consumer.*.status.successBatchGroup.topic.subscription.2xx.200.count"), 1);
        graphiteServer.expectMetric(metricNameWithPrefix("consumer.*.status.successBatchGroup.topic.subscription.2xx.count"), 1);
        remoteService.expectMessages(simpleJson(), simpleJson());

        // when
        publisher.publish(topic.getQualifiedName(), simpleJson());
        publisher.publish(topic.getQualifiedName(), simpleJson());

        // then
        graphiteServer.waitUntilReceived();
    }

    @Test
    public void shouldReportMetricsForFailedBatchDelivery() {
        // given
        Topic topic = operations.buildTopic("errorBatchGroup", "topic");
        operations.createSubscription(topic, subscription(topic, "subscription")
                .withEndpoint(HTTP_ENDPOINT_URL)
                .withSubscriptionPolicy(batchSubscriptionPolicy()
                        .withBatchSize(2)
                        .withMessageTtl(0)
                        .withRequestTimeout(MAX_VALUE)
                        .withBatchTime(MAX_VALUE)
                        .withBatchVolume(1024)
                        .build())
                .build());

        remoteService.setReturnedStatusCode(404);
        graphiteServer.expectMetric(metricNameWithPrefix("consumer.*.status.errorBatchGroup.topic.subscription.4xx.404.count"), 1);
        graphiteServer.expectMetric(metricNameWithPrefix("consumer.*.status.errorBatchGroup.topic.subscription.4xx.count"), 1);
        remoteService.expectMessages(simpleJson(), simpleJson());

        // when
        publisher.publish(topic.getQualifiedName(), simpleJson());
        publisher.publish(topic.getQualifiedName(), simpleJson());

        // then
        graphiteServer.waitUntilReceived();
    }

    private String metricNameWithPrefix(String metricName) {
        return String.format("%s.%s", Configs.GRAPHITE_PREFIX.getDefaultValue(), metricName);
    }

    private static String jsonWithField(String key, Object value) {
        return TestMessage.of(key, value).body();
    }

    private static String simpleJson() {
        return TestMessage.simple().body();
    }

    private static MessageFilterSpecification filterMatchingHeaderByPattern(String headerName, String pattern) {
        return new MessageFilterSpecification(of("type", "header", "header", headerName, "matcher", pattern));
    }
}
