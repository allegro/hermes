package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringSubscriptionMetricsProvider;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringTopicMetricsProvider;

import java.util.List;
import java.util.stream.Stream;
import pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusClient.Query;

public class VictoriaMetricsMetricsProvider implements MonitoringSubscriptionMetricsProvider, MonitoringTopicMetricsProvider {

    private static final String SUBSCRIPTION_DELIVERED = "subscription_delivered_total";
    private static final String SUBSCRIPTION_TIMEOUTS = "subscription_timeouts_total";
    private static final String SUBSCRIPTION_THROUGHPUT = "subscription_throughput_bytes_total";
    private static final String SUBSCRIPTION_OTHER_ERRORS = "subscription_other_errors_total";
    private static final String SUBSCRIPTION_BATCHES = "subscription_batches_total";
    private static final String SUBSCRIPTION_STATUS_CODES = "subscription_http_status_codes_total";
    private static final String SUBSCRIPTION_STATUS_CODES_2XX = SUBSCRIPTION_STATUS_CODES + "_2xx";
    private static final String SUBSCRIPTION_STATUS_CODES_4XX = SUBSCRIPTION_STATUS_CODES + "_4xx";
    private static final String SUBSCRIPTION_STATUS_CODES_5XX = SUBSCRIPTION_STATUS_CODES + "_5xx";
    private static final String SUBSCRIPTION_RETRIES = "subscription_retries_total";

    private static final String TOPIC_RATE = "topic_requests_total";
    private static final String TOPIC_DELIVERY_RATE = "subscription_delivered_total";
    private static final String TOPIC_THROUGHPUT_RATE = "topic_throughput_bytes_total";

    private final String consumersMetricsPrefix;
    private final String frontendMetricsPrefix;
    private final String additionalFilters;
    private final Stream<String> subscriptionMetricsToQuery;
    private final Stream<String> topicMetricsToQuery;
    private final PrometheusClient prometheusClient;

    public VictoriaMetricsMetricsProvider(PrometheusClient prometheusClient, String consumersMetricsPrefix,
                                          String frontendMetricsPrefix, String additionalFilters) {
        this.prometheusClient = prometheusClient;
        this.consumersMetricsPrefix = consumersMetricsPrefix.isEmpty() ? "" : consumersMetricsPrefix + "_";
        this.frontendMetricsPrefix = frontendMetricsPrefix.isEmpty() ? "" : frontendMetricsPrefix + "_";
        this.additionalFilters = additionalFilters;
        this.subscriptionMetricsToQuery = Stream.of(SUBSCRIPTION_DELIVERED, SUBSCRIPTION_TIMEOUTS, SUBSCRIPTION_RETRIES,
                        SUBSCRIPTION_THROUGHPUT, SUBSCRIPTION_OTHER_ERRORS, SUBSCRIPTION_BATCHES, SUBSCRIPTION_STATUS_CODES)
                .map(this::consumerMetricName);
        this.topicMetricsToQuery = Stream.of(
                frontendMetricName(TOPIC_RATE),
                consumerMetricName(TOPIC_DELIVERY_RATE),
                frontendMetricName(TOPIC_THROUGHPUT_RATE));
    }

    @Override
    public MonitoringSubscriptionMetrics subscriptionMetrics(SubscriptionName subscriptionName) {
        List<Query> queries = this.subscriptionMetricsToQuery
                .map(queryName -> Query.forSubscription(queryName, subscriptionName, additionalFilters))
                .toList();

        MonitoringMetricsContainer prometheusMetricsContainer = prometheusClient.readMetrics(queries);
        return MonitoringSubscriptionMetricsProvider
                .metricsBuilder()
                .withRate(prometheusMetricsContainer.metricValue(consumerMetricName(SUBSCRIPTION_DELIVERED)))
                .withTimeouts(prometheusMetricsContainer.metricValue(consumerMetricName(SUBSCRIPTION_TIMEOUTS)))
                .withThroughput(prometheusMetricsContainer.metricValue(consumerMetricName(SUBSCRIPTION_THROUGHPUT)))
                .withOtherErrors(prometheusMetricsContainer.metricValue(consumerMetricName(SUBSCRIPTION_OTHER_ERRORS)))
                .withMetricPathBatchRate(prometheusMetricsContainer.metricValue(consumerMetricName(SUBSCRIPTION_BATCHES)))
                .withCodes2xx(prometheusMetricsContainer.metricValue(consumerMetricName(SUBSCRIPTION_STATUS_CODES_2XX)))
                .withCode4xx(prometheusMetricsContainer.metricValue(consumerMetricName(SUBSCRIPTION_STATUS_CODES_4XX)))
                .withCode5xx(prometheusMetricsContainer.metricValue(consumerMetricName(SUBSCRIPTION_STATUS_CODES_5XX)))
                .withRetries(prometheusMetricsContainer.metricValue(consumerMetricName(SUBSCRIPTION_RETRIES)))
                .build();
    }

    @Override
    public MonitoringTopicMetrics topicMetrics(TopicName topicName) {
        List<Query> queries = topicMetricsToQuery.map(queryName -> Query.forTopic(queryName, topicName, additionalFilters)).toList();

        MonitoringMetricsContainer prometheusMetricsContainer = prometheusClient.readMetrics(queries);
        return MonitoringTopicMetricsProvider
                .metricsBuilder()
                .withRate(prometheusMetricsContainer.metricValue(frontendMetricName(TOPIC_RATE)))
                .withDeliveryRate(prometheusMetricsContainer.metricValue(consumerMetricName(TOPIC_DELIVERY_RATE)))
                .withThroughput(prometheusMetricsContainer.metricValue(frontendMetricName(TOPIC_THROUGHPUT_RATE)))
                .build();
    }

    private String consumerMetricName(String name) {
        return consumersMetricsPrefix + name;
    }

    private String frontendMetricName(String name) {
        return frontendMetricsPrefix + name;
    }
}