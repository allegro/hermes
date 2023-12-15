package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionNameWithMetrics;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.integration.helper.PrometheusEndpoint;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static pl.allegro.tech.hermes.integration.helper.PrometheusEndpoint.PrometheusSubscriptionResponseBuilder.builder;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class QueryEndpointTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    private PrometheusEndpoint prometheusEndpoint;

    @BeforeClass
    public void initialize() {
        remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
        prometheusEndpoint = new PrometheusEndpoint(SharedServices.services().prometheusHttpMock());
    }

    @AfterClass
    public void tearDown() {
        remoteService.reset();
    }

    @Test
    public void shouldQuerySubscriptionsMetrics() {
        // given
        Topic topic1 = operations.buildTopic("subscriptionsMetricsTestGroup1", "topic");
        Topic topic2 = operations.buildTopic("subscriptionsMetricsTestGroup2", "topic");

        Subscription subscription1 = operations.createSubscription(topic1, "subscription1", remoteService.getUrl());
        Subscription subscription2 = operations.createSubscription(topic2, "subscription2", remoteService.getUrl());

        String queryGetAllSubscriptionsMetrics = "{\"query\": {}}";
        String queryGetSubscriptionsMetricsWithPositiveThroughput = "{\"query\": {\"throughput\": {\"gt\": 0}}}";
        String queryGetSubscriptionsMetricsWithRateInRange = "{\"query\": {\"or\": [{\"rate\": {\"gt\": 10}}, {\"rate\": {\"lt\": 50}}]}}";
        String queryGetSubscriptionsMetricsWithLagNegative = "{\"query\": {\"lag\": {\"lt\": 0}}}";
        String queryGetSubscriptionsMetricsWithVolume = "{\"query\": {\"volume\": {\"gt\": -1}}}";

        prometheusEndpoint.returnSubscriptionMetrics(topic1, "subscription1", builder()
                .withRate(100).withThroughput(0).build());
        prometheusEndpoint.returnSubscriptionMetrics(topic2, "subscription2", builder()
                .withRate(40).withThroughput(10).build());

        wait.until(() -> {
            // when
            final List<SubscriptionNameWithMetrics> allSubscriptions = management.query()
                    .querySubscriptionsMetrics(queryGetAllSubscriptionsMetrics);
            final List<SubscriptionNameWithMetrics> subscriptionsWithPositiveThroughput = management.query()
                    .querySubscriptionsMetrics(queryGetSubscriptionsMetricsWithPositiveThroughput);
            final List<SubscriptionNameWithMetrics> subscriptionsWithRateInRange = management.query()
                    .querySubscriptionsMetrics(queryGetSubscriptionsMetricsWithRateInRange);
            final List<SubscriptionNameWithMetrics> subscriptionsWithNegativeLag = management.query()
                    .querySubscriptionsMetrics(queryGetSubscriptionsMetricsWithLagNegative);
            final List<SubscriptionNameWithMetrics> subscriptionsWithVolume = management.query()
                    .querySubscriptionsMetrics(queryGetSubscriptionsMetricsWithVolume);

            // then
            subscriptionsMatchesToNamesAndTheirTopicsNames(allSubscriptions, subscription1, subscription2);
            subscriptionsMatchesToNamesAndTheirTopicsNames(subscriptionsWithPositiveThroughput, subscription2);
            subscriptionsMatchesToNamesAndTheirTopicsNames(subscriptionsWithRateInRange, subscription2);
            subscriptionsMatchesToNamesAndTheirTopicsNames(subscriptionsWithNegativeLag, subscription1, subscription2);
            subscriptionsMatchesToNamesAndTheirTopicsNames(subscriptionsWithVolume, subscription1, subscription2);
        });
    }

    @Test
    public void shouldHandleUnavailableSubscriptionsMetrics() {
        // given
        Topic topic = operations.buildTopic("unavailableMetricsGroup", "topic");
        Subscription subscription = operations.createSubscription(topic, "subscription", remoteService.getUrl());

        String queryGetAllSubscriptionsMetrics = "{\"query\": {}}";
        String queryGetSubscriptionsMetricsWithPositiveRate = "{\"query\": {\"rate\": {\"gt\": 0}}}";

        int prometheusResponseDelay = 10 * 60 * 1000;
        prometheusEndpoint.returnSubscriptionMetricsWithDelay(topic, "subscription",
                builder()
                .withRate(100)
                .build(),
                prometheusResponseDelay
        );

        wait.until(() -> {
            // when
            List<SubscriptionNameWithMetrics> allSubscriptions = management.query()
                    .querySubscriptionsMetrics(queryGetAllSubscriptionsMetrics);
            List<SubscriptionNameWithMetrics> subscriptionsWithPositiveRate = management.query()
                    .querySubscriptionsMetrics(queryGetSubscriptionsMetricsWithPositiveRate);

            // then
            assertThatRateIsUnavailable(allSubscriptions, subscription);
            assertThatRateIsUnavailable(subscriptionsWithPositiveRate, subscription);
        });
    }

    private static void assertThatRateIsUnavailable(List<SubscriptionNameWithMetrics> allSubscriptions, Subscription ... subscriptions) {
        subscriptionsMatchesToNamesAndTheirTopicsNames(allSubscriptions, subscriptions);
        for (SubscriptionNameWithMetrics metrics : allSubscriptions) {
            assertThat(metrics.getRate().asString()).isEqualTo("unavailable");
        }
    }

    private static void subscriptionsMatchesToNamesAndTheirTopicsNames(List<SubscriptionNameWithMetrics> found,
                                                                       Subscription ... expectedSubscriptions) {

        Map<String, String> foundSubscriptionsAndTheirTopicNames = found.stream()
                .collect(Collectors.toMap(SubscriptionNameWithMetrics::getName, SubscriptionNameWithMetrics::getTopicName));

        for (Subscription subscription : expectedSubscriptions) {
            assertThat(foundSubscriptionsAndTheirTopicNames).containsKeys(subscription.getName());
            assertThat(foundSubscriptionsAndTheirTopicNames.get(subscription.getName())).isEqualTo(subscription.getQualifiedTopicName());
        }
    }
}
