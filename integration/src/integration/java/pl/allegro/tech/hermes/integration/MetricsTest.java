package pl.allegro.tech.hermes.integration;

import com.googlecode.catchexception.CatchException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
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

import static com.googlecode.catchexception.CatchException.catchException;
import static org.assertj.core.api.Assertions.assertThat;
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
    @Test(enabled = false)
    public void shouldIncreaseTopicMetricsAfterMessageHasBeenPublished() {
        // given
        Topic topic = operations.buildTopic("topicMetricsGroup", "topic");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        graphiteEndpoint.returnMetricForTopic("topicMetricsGroup", "topic", 10, 15);

        remoteService.expectMessages(TestMessage.simple().body());
        publisher.publish("topicMetricsGroup.topic", TestMessage.simple().body());
        remoteService.waitUntilReceived();

        wait.until(() -> {
            // when
            TopicMetrics metrics = management.topic().getMetrics("topicMetricsGroup.topic");

            // then
            assertThat(metrics.getRate()).isEqualTo("10");
            assertThat(metrics.getDeliveryRate()).isEqualTo("15");
            assertThat(metrics.getPublished()).isEqualTo(1);
        });
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldIncreaseSubscriptionDeliveredMetricsAfterMessageDelivered() {
        // given
        Topic topic = operations.buildTopic("subscriptionMetricsGroup", "topic");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        graphiteEndpoint.returnMetricForSubscription("subscriptionMetricsGroup", "topic", "subscription", 15);

        remoteService.expectMessages(TestMessage.simple().body());
        publisher.publish("subscriptionMetricsGroup.topic", TestMessage.simple().body());
        remoteService.waitUntilReceived();

        wait.untilPublishedMetricsPropagation();

        // when
        SubscriptionMetrics metrics = management.subscription().getMetrics("subscriptionMetricsGroup.topic", "subscription");

        // then
        assertThat(metrics.getRate()).isEqualTo("15");
        //we have same instance of metric registry in frontend and consumer, so metrics reporting is duplicated
        assertThat(metrics.getDelivered()).isEqualTo(2);
        assertThat(metrics.getDiscarded()).isEqualTo(0);
        assertThat(metrics.getInflight()).isEqualTo(0);
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldNotCreateNewSubscriptionWhenAskedForNonExistingMetrics() {
        //given
        TopicName topic = new TopicName("pl.group.sub.bug", "topic");
        operations.buildTopic(topic.getGroupName(), topic.getName());
        String randomSubscription = UUID.randomUUID().toString();

        //when
        catchException(management.subscription())
                .getMetrics(topic.qualifiedName(), randomSubscription);

        //then
        assertThat(management.subscription().list(topic.qualifiedName(), false)).doesNotContain(randomSubscription);
        assertThat(CatchException.<BadRequestException>caughtException())
                .isInstanceOf(BadRequestException.class);
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldReadSubscriptionDeliveryRate() {
        Topic topic = operations.buildTopic("pl.allegro.tech.hermes", "topic");
        operations.createSubscription(topic, "pl.allegro.tech.hermes.subscription", HTTP_ENDPOINT_URL);
        graphiteEndpoint.returnMetricForSubscription("pl_allegro_tech_hermes", "topic", "pl_allegro_tech_hermes_subscription", 15);

        SubscriptionMetrics metrics = management.subscription().getMetrics("pl.allegro.tech.hermes.topic", "pl.allegro.tech.hermes.subscription");
        assertThat(metrics.getRate()).isEqualTo("15");
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldSendMetricToGraphite() {
        //given
        operations.buildTopic("metricGroup", "topic");
        graphiteServer.expectMetric(metricNameWithPrefix("producer.*.meter.metricGroup.topic.count"), 1);

        //when
        publisher.publish("metricGroup.topic", TestMessage.simple().body());

        //then
        graphiteServer.waitUntilReceived();
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldNotCreateNewTopicWhenAskingForNonExistingMetrics() {
        //given
        TopicName newTopic = new TopicName("auto-topic-bug", "not-existing");
        operations.createGroup(newTopic.getGroupName());

        //when
        management.topic().getMetrics(newTopic.qualifiedName());

        //then
        assertThat(management.topic().list(newTopic.getGroupName(), false)).doesNotContain(newTopic.qualifiedName());
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldNotReportMetricsToConfigStorageForRemovedSubscription() {
        //given
        Topic topic = operations.buildTopic("metricsAfterSubscriptionRemovedGroup", "topic");
        String subscriptionName1 = "subscription";
        operations.createSubscription(topic, subscriptionName1, HTTP_ENDPOINT_URL);
        remoteService.expectMessages(TestMessage.simple().body());

        publisher.publish(topic.getQualifiedName(), TestMessage.simple().body());
        remoteService.waitUntilReceived();

        wait.untilSubscriptionMetricsIsCreated(topic.getName(), subscriptionName1);

        //when
        management.subscription().remove(topic.getQualifiedName(), subscriptionName1);

        //then
        wait.untilSubscriptionMetricsIsRemoved(topic.getName(), subscriptionName1);

        String subscriptionName2 = "subscription2";
        operations.createSubscription(topic, subscriptionName2, HTTP_ENDPOINT_URL);
        management.topic().publishMessage(topic.getQualifiedName(), TestMessage.simple().body());
        wait.untilSubscriptionMetricsIsCreated(topic.getName(), subscriptionName2);
        wait.untilSubscriptionMetricsIsRemoved(topic.getName(), subscriptionName1);
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldReportHttpErrorCodeMetrics() {
        //given
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

        //when
        publisher.publish(topic.getQualifiedName(), TestMessage.simple().body());

        //then
        graphiteServer.waitUntilReceived();
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldReportHttpSuccessCodeMetrics() {
        //given
        Topic topic = operations.buildTopic("statusSuccessGroup", "topic");
        String subscriptionName = "subscription";
        operations.createSubscription(topic, subscriptionName, HTTP_ENDPOINT_URL);
        graphiteServer.expectMetric(metricNameWithPrefix("consumer.*.status.statusSuccessGroup.topic.subscription.2xx.200.count"), 1);
        graphiteServer.expectMetric(metricNameWithPrefix("consumer.*.status.statusSuccessGroup.topic.subscription.2xx.count"), 1);
        remoteService.expectMessages(TestMessage.simple().body());

        //when
        publisher.publish(topic.getQualifiedName(), TestMessage.simple().body());

        //then
        graphiteServer.waitUntilReceived();
    }

    private String metricNameWithPrefix(String metricName) {
        return String.format("%s.%s", Configs.GRAPHITE_PREFIX.getDefaultValue(), metricName);
    }
}
