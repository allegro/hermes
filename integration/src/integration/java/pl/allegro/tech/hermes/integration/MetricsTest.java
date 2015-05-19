package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.helper.GraphiteEndpoint;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.integration.helper.graphite.GraphiteMockServer;
import pl.allegro.tech.hermes.integration.shame.Unreliable;

import static org.assertj.core.api.Assertions.assertThat;

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
        operations.buildSubscription("topicMetricsGroup", "topic", "subscription", HTTP_ENDPOINT_URL);
        wait.untilSubscriptionIsCreated("topicMetricsGroup", "topic", "subscription");
        graphiteEndpoint.returnMetricForTopic("topicMetricsGroup", "topic", 10, 15);

        remoteService.expectMessages(TestMessage.simple().body());
        publisher.publish("topicMetricsGroup.topic", TestMessage.simple().body());
        remoteService.waitUntilReceived();

        wait.untilPublishedMetricsPropagation();

        // when
        TopicMetrics metrics = management.topic().getMetrics("topicMetricsGroup.topic");

        // then
        assertThat(metrics.getRate()).isEqualTo("10");
        assertThat(metrics.getDeliveryRate()).isEqualTo("15");
        assertThat(metrics.getPublished()).isEqualTo(1);
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldIncreaseSubscriptionDeliveredMetricsAfterMessageDelivered() {
        // given
        operations.buildSubscription("subscriptionMetricsGroup", "topic", "subscription", HTTP_ENDPOINT_URL);
        wait.untilSubscriptionIsCreated("subscriptionMetricsGroup", "topic", "subscription");
        graphiteEndpoint.returnMetricForSubscription("subscriptionMetricsGroup", "topic", "subscription", 15);

        remoteService.expectMessages(TestMessage.simple().body());
        publisher.publish("subscriptionMetricsGroup.topic", TestMessage.simple().body());
        remoteService.waitUntilReceived();

        wait.untilPublishedMetricsPropagation();

        // when
        SubscriptionMetrics metrics = management.subscription().getMetrics("subscriptionMetricsGroup.topic", "subscription");

        // then
        assertThat(metrics.getRate()).isEqualTo("15");
        assertThat(metrics.getDelivered()).isEqualTo(2); //we have same instance of metric registry in frontend and consumer, so metrics reporting is duplicated
        assertThat(metrics.getDiscarded()).isEqualTo(0);
        assertThat(metrics.getInflight()).isEqualTo(0);
    }

    @Test
    public void shouldReadSubscriptionDeliveryRate() {
        operations.buildSubscription("pl.allegro.tech.hermes", "topic", "pl.allegro.tech.hermes.subscription", HTTP_ENDPOINT_URL);
        wait.untilSubscriptionIsCreated("pl.allegro.tech.hermes", "topic", "pl.allegro.tech.hermes.subscription");
        graphiteEndpoint.returnMetricForSubscription("pl_allegro_tech_hermes", "topic", "pl_allegro_tech_hermes_subscription", 15);

        SubscriptionMetrics metrics = management.subscription().getMetrics("pl.allegro.tech.hermes.topic", "pl.allegro.tech.hermes.subscription");
        assertThat(metrics.getRate()).isEqualTo("15");
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldIncreasePublishedMetricsIncrementallyInMultipleMetricUpdateCycles() {
        // given
        operations.buildSubscription("incrementalMetricsGroup", "topic", "subscription", HTTP_ENDPOINT_URL);
        wait.untilSubscriptionIsCreated("incrementalMetricsGroup", "topic", "subscription");
        graphiteEndpoint.returnMetricForTopic("incrementalMetricsGroup", "topic", 10, 15);

        remoteService.expectMessages(TestMessage.simple().body(), TestMessage.simple().body());
        publisher.publish("incrementalMetricsGroup.topic", TestMessage.simple().body());
        wait.untilPublishedMetricsPropagation();

        publisher.publish("incrementalMetricsGroup.topic", TestMessage.simple().body());
        wait.untilPublishedMetricsPropagation();

        // when
        TopicMetrics metrics = management.topic().getMetrics("incrementalMetricsGroup.topic");

        // then
        remoteService.waitUntilReceived();
        assertThat(metrics.getPublished()).isEqualTo(2);
    }

    @Test
    public void shouldSendMetricToGraphite() {
        //given
        operations.buildTopic("metricGroup", "topic");
        graphiteServer.expectMetric(metricNameWithPrefix("producer.*.meter.metricGroup.topic.count"), 1);

        //when
        publisher.publish("metricGroup.topic", TestMessage.simple().body());

        //then
        graphiteServer.waitUntilReceived();
    }

    @Test
    public void shouldSendLagMetricsToGraphite() {
        //given
        operations.buildSubscription("lagMetricGroup", "topic", "subscription", HTTP_ENDPOINT_URL);
        wait.untilSubscriptionIsCreated("lagMetricGroup", "topic", "subscription");
        remoteService.expectMessages(TestMessage.simple().body());
        graphiteServer.expectMetric(metricNameWithPrefix("consumer.offset.lagMetricGroup.topic.subscription.[0-9].lag.count"), 1);
        graphiteServer.expectMetric(metricNameWithPrefix("consumer.offset.lagMetricGroup.topic.subscription.[0-9].timeLag.count"), 1);

        //when
        publisher.publish("lagMetricGroup.topic", TestMessage.simple().body());

        //then
        remoteService.waitUntilReceived();
        graphiteServer.waitUntilReceived();
    }

    @Test
    public void shouldNotCreateNewTopicWhenAskingForNonExistingMetrics() {
        //given
        TopicName newTopic = new TopicName("auto-topic-bug", "not-existing");
        operations.createGroup(newTopic.getGroupName());

        //when
        management.topic().getMetrics(newTopic.qualifiedName());

        //then
        assertThat(management.topic().list(newTopic.getGroupName())).doesNotContain(newTopic.qualifiedName());
    }

    @Test
    public void shouldNotReportMetricsToConfigStorageForRemovedSubscription() {
        //given
        TopicName topicName = TopicName.fromQualifiedName("metricsAfterSubscriptionRemovedGroup.topic");
        String subscriptionName1 = "subscription";
        operations.buildSubscription(topicName.getGroupName(), topicName.getName(), subscriptionName1, HTTP_ENDPOINT_URL);
        wait.untilSubscriptionIsCreated(topicName.getGroupName(), topicName.getName(), subscriptionName1);
        remoteService.expectMessages(TestMessage.simple().body());

        publisher.publish(topicName.qualifiedName(), TestMessage.simple().body());
        remoteService.waitUntilReceived();

        wait.untilSubscriptionMetricsIsCreated(topicName, subscriptionName1);

        //when
        management.subscription().remove(topicName.qualifiedName(), subscriptionName1);

        //then
        wait.untilSubscriptionMetricsIsRemoved(topicName, subscriptionName1);

        String subscriptionName2 = "subscription2";
        operations.buildSubscription(topicName.getGroupName(), topicName.getName(), subscriptionName2, HTTP_ENDPOINT_URL);
        wait.untilSubscriptionIsCreated(topicName.getGroupName(), topicName.getName(), subscriptionName2);
        management.topic().publishMessage(topicName.qualifiedName(), TestMessage.simple().body());
        wait.untilSubscriptionMetricsIsCreated(topicName, subscriptionName2);
        wait.untilSubscriptionMetricsIsRemoved(topicName, subscriptionName1);
    }

    private String metricNameWithPrefix(String metricName) {
        return String.format("%s.%s", Configs.GRAPHITE_PREFIX.getDefaultValue(), metricName);
    }
}
