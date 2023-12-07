package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.MetricDecimalValue;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.helper.PrometheusEndpoint;
import pl.allegro.tech.hermes.integration.helper.PrometheusEndpoint.PrometheusTopicResponse;
import pl.allegro.tech.hermes.integration.shame.Unreliable;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import static jakarta.ws.rs.core.Response.Status.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.integration.helper.PrometheusEndpoint.PrometheusSubscriptionResponseBuilder.builder;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class MetricsTest extends IntegrationTest {

    private PrometheusEndpoint prometheusEndpoint;

    private RemoteServiceEndpoint remoteService;

    @BeforeMethod
    public void initializeAlways() {
        this.prometheusEndpoint = new PrometheusEndpoint(SharedServices.services().prometheusHttpMock());
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Unreliable
    @Test
    public void shouldReadTopicMetricsFromPrometheus() {
        // given
        Topic topic = operations.buildTopic(randomTopic("group", "topic_metrics").build());
        prometheusEndpoint.returnTopicMetrics(topic, new PrometheusTopicResponse(10, 15, 0));

        wait.until(() -> {
            // when
            TopicMetrics metrics = management.topic().getMetrics(topic.getQualifiedName());

            // then
            assertThat(metrics.getRate()).isEqualTo(MetricDecimalValue.of("10.0"));
            assertThat(metrics.getDeliveryRate()).isEqualTo(MetricDecimalValue.of("15.0"));
        });
    }

    @Unreliable
    @Test
    public void shouldReadSubscriptionMetricsFromPrometheus() {
        // given
        Topic topic = operations.buildTopic(randomTopic("pl.group", "topic").build());
        operations.createSubscription(topic, "subscription", remoteService.getUrl());
        prometheusEndpoint.returnSubscriptionMetrics(topic, "subscription", builder().withRate(15).build());

        wait.until(() -> {
            // when
            SubscriptionMetrics metrics = management.subscription().getMetrics(topic.getQualifiedName(), "subscription");

            // then
            assertThat(metrics.getRate()).isEqualTo(MetricDecimalValue.of("15.0"));
            assertThat(metrics.getDelivered()).isEqualTo(1);
            assertThat(metrics.getDiscarded()).isEqualTo(0);
            assertThat(metrics.getVolume()).isGreaterThan(1);
        });
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldNotReportMetricsToConfigStorageForRemovedSubscription() {
        // given
        Topic topic = operations.buildTopic("metricsAfterSubscriptionRemovedGroup", "topic");
        String subscriptionName1 = "subscription";
        operations.createSubscription(topic, subscriptionName1, remoteService.getUrl());
        remoteService.expectMessages(TestMessage.simple().body());

        assertThat(publisher.publish(topic.getQualifiedName(), TestMessage.simple().body())).isEqualTo(CREATED);
        remoteService.waitUntilReceived();

        wait.untilSubscriptionMetricsIsCreated(topic.getName(), subscriptionName1);

        // when
        management.subscription().remove(topic.getQualifiedName(), subscriptionName1);

        // then
        wait.untilSubscriptionMetricsIsRemoved(topic.getName(), subscriptionName1);

        String subscriptionName2 = "subscription2";
        operations.createSubscription(topic, subscriptionName2, remoteService.getUrl());
        management.topic().publishMessage(topic.getQualifiedName(), TestMessage.simple().body());
        wait.untilSubscriptionMetricsIsCreated(topic.getName(), subscriptionName2);
        wait.untilSubscriptionMetricsIsRemoved(topic.getName(), subscriptionName1);
    }
}
