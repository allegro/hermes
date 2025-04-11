package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import static pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusClient.forSubscription;
import static pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusClient.forSubscriptionHistogram;
import static pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusClient.forSubscriptionStatusCode;
import static pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusClient.forTopic;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringSubscriptionMetricsProvider;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringTopicMetricsProvider;

public class PrometheusMetricsProvider
    implements MonitoringSubscriptionMetricsProvider, MonitoringTopicMetricsProvider {

  private static final String SUBSCRIPTION_DELIVERED = "subscription_delivered_total";
  private static final String SUBSCRIPTION_TIMEOUTS = "subscription_timeouts_total";
  private static final String SUBSCRIPTION_THROUGHPUT = "subscription_throughput_bytes_total";
  private static final String SUBSCRIPTION_OTHER_ERRORS = "subscription_other_errors_total";
  private static final String SUBSCRIPTION_BATCHES = "subscription_batches_total";
  private static final String SUBSCRIPTION_STATUS_CODES = "subscription_http_status_codes_total";
  private static final String SUBSCRIPTION_RETRIES = "subscription_retries_total";
  private static final String SUBSCRIPTION_MESSAGE_PROCESSING_TIME =
      "subscription_message_processing_time_seconds_bucket";

  private static final String TOPIC_RATE = "topic_requests_total";
  private static final String TOPIC_DELIVERY_RATE = "subscription_delivered_total";
  private static final String TOPIC_THROUGHPUT_RATE = "topic_throughput_bytes_total";

  private final String consumersMetricsPrefix;
  private final String frontendMetricsPrefix;
  private final String additionalFilters;
  private final PrometheusClient prometheusClient;

  public PrometheusMetricsProvider(
      PrometheusClient prometheusClient,
      String consumersMetricsPrefix,
      String frontendMetricsPrefix,
      String additionalFilters) {
    this.prometheusClient = prometheusClient;
    this.consumersMetricsPrefix =
        consumersMetricsPrefix.isEmpty() ? "" : consumersMetricsPrefix + "_";
    this.frontendMetricsPrefix = frontendMetricsPrefix.isEmpty() ? "" : frontendMetricsPrefix + "_";
    this.additionalFilters = additionalFilters;
  }

  @Override
  public MonitoringSubscriptionMetrics subscriptionMetrics(SubscriptionName subscriptionName) {
    String subscriptionDeliveredQuery =
        forSubscription(
            consumerMetricName(SUBSCRIPTION_DELIVERED), subscriptionName, additionalFilters);
    String subscriptionTimeoutsQuery =
        forSubscription(
            consumerMetricName(SUBSCRIPTION_TIMEOUTS), subscriptionName, additionalFilters);
    String subscriptionThroughputQuery =
        forSubscription(
            consumerMetricName(SUBSCRIPTION_THROUGHPUT), subscriptionName, additionalFilters);
    String subscriptionOtherErrorsQuery =
        forSubscription(
            consumerMetricName(SUBSCRIPTION_OTHER_ERRORS), subscriptionName, additionalFilters);
    String subscriptionBatchesQuery =
        forSubscription(
            consumerMetricName(SUBSCRIPTION_BATCHES), subscriptionName, additionalFilters);
    String subscriptionRetriesQuery =
        forSubscription(
            consumerMetricName(SUBSCRIPTION_RETRIES), subscriptionName, additionalFilters);
    String subscription2xx =
        forSubscriptionStatusCode(
            consumerMetricName(SUBSCRIPTION_STATUS_CODES),
            subscriptionName,
            "2.*",
            additionalFilters);
    String subscription4xx =
        forSubscriptionStatusCode(
            consumerMetricName(SUBSCRIPTION_STATUS_CODES),
            subscriptionName,
            "4.*",
            additionalFilters);
    String subscription5xx =
        forSubscriptionStatusCode(
            consumerMetricName(SUBSCRIPTION_STATUS_CODES),
            subscriptionName,
            "5.*",
            additionalFilters);
    String subscriptionMessageProcessingTimeQuery =
        forSubscriptionHistogram(
            consumerMetricName(SUBSCRIPTION_MESSAGE_PROCESSING_TIME),
            subscriptionName,
            additionalFilters);

    MonitoringMetricsContainer prometheusMetricsContainer =
        prometheusClient.readMetrics(
            subscriptionDeliveredQuery,
            subscriptionTimeoutsQuery,
            subscriptionRetriesQuery,
            subscriptionThroughputQuery,
            subscriptionOtherErrorsQuery,
            subscriptionBatchesQuery,
            subscription2xx,
            subscription4xx,
            subscription5xx,
            subscriptionMessageProcessingTimeQuery);
    return MonitoringSubscriptionMetricsProvider.metricsBuilder()
        .withRate(prometheusMetricsContainer.metricValue(subscriptionDeliveredQuery))
        .withTimeouts(prometheusMetricsContainer.metricValue(subscriptionTimeoutsQuery))
        .withThroughput(prometheusMetricsContainer.metricValue(subscriptionThroughputQuery))
        .withOtherErrors(prometheusMetricsContainer.metricValue(subscriptionOtherErrorsQuery))
        .withMetricPathBatchRate(prometheusMetricsContainer.metricValue(subscriptionBatchesQuery))
        .withCodes2xx(prometheusMetricsContainer.metricValue(subscription2xx))
        .withCode4xx(prometheusMetricsContainer.metricValue(subscription4xx))
        .withCode5xx(prometheusMetricsContainer.metricValue(subscription5xx))
        .withRetries(prometheusMetricsContainer.metricValue(subscriptionRetriesQuery))
        .withMessageProcessingTime(
            prometheusMetricsContainer.metricHistogramValue(subscriptionMessageProcessingTimeQuery))
        .build();
  }

  @Override
  public MonitoringTopicMetrics topicMetrics(TopicName topicName) {
    String topicRateQuery = forTopic(frontendMetricName(TOPIC_RATE), topicName, additionalFilters);
    String topicDeliveryRateQuery =
        forTopic(consumerMetricName(TOPIC_DELIVERY_RATE), topicName, additionalFilters);
    String topicThroughputQuery =
        forTopic(frontendMetricName(TOPIC_THROUGHPUT_RATE), topicName, additionalFilters);

    MonitoringMetricsContainer prometheusMetricsContainer =
        prometheusClient.readMetrics(topicRateQuery, topicDeliveryRateQuery, topicThroughputQuery);
    return MonitoringTopicMetricsProvider.metricsBuilder()
        .withRate(prometheusMetricsContainer.metricValue(topicRateQuery))
        .withDeliveryRate(prometheusMetricsContainer.metricValue(topicDeliveryRateQuery))
        .withThroughput(prometheusMetricsContainer.metricValue(topicThroughputQuery))
        .build();
  }

  private String consumerMetricName(String name) {
    return consumersMetricsPrefix + name;
  }

  private String frontendMetricName(String name) {
    return frontendMetricsPrefix + name;
  }
}
