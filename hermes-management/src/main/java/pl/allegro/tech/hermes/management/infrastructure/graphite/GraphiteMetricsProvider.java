package pl.allegro.tech.hermes.management.infrastructure.graphite;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringSubscriptionMetricsProvider;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringTopicMetricsProvider;

import static pl.allegro.tech.hermes.common.metric.HermesMetrics.escapeDots;

public class GraphiteMetricsProvider implements MonitoringSubscriptionMetricsProvider, MonitoringTopicMetricsProvider {

    private static final String SUBSCRIPTION_PATH = "%s.%s.%s";

    private static final String SUBSCRIPTION_RATE_PATTERN = "sumSeries(%s.consumer.*.meter.%s.m1_rate)";
    private static final String SUBSCRIPTION_THROUGHPUT_PATTERN = "sumSeries(%s.consumer.*.throughput.%s.m1_rate)";
    private static final String SUBSCRIPTION_HTTP_STATUSES_PATTERN = "sumSeries(%s.consumer.*.status.%s.%s.m1_rate)";
    private static final String SUBSCRIPTION_ERROR_TIMEOUT_PATTERN = "sumSeries(%s.consumer.*.status.%s.errors.timeout.m1_rate)";
    private static final String SUBSCRIPTION_ERROR_OTHER_PATTERN = "sumSeries(%s.consumer.*.status.%s.errors.other.m1_rate)";
    private static final String SUBSCRIPTION_BATCH_RATE_PATTERN = "sumSeries(%s.consumer.*.meter.%s.batch.m1_rate)";

    private static final String TOPIC_RATE_PATTERN = "sumSeries(%s.producer.*.meter.%s.%s.m1_rate)";
    private static final String TOPIC_DELIVERY_RATE_PATTERN = "sumSeries(%s.consumer.*.meter.%s.%s.m1_rate)";
    private static final String TOPIC_THROUGHPUT_PATTERN = "sumSeries(%s.producer.*.throughput.%s.%s.m1_rate)";

    private final GraphiteClient graphiteClient;
    private final String prefix;

    public GraphiteMetricsProvider(GraphiteClient graphiteClient, String prefix) {
        this.graphiteClient = graphiteClient;
        this.prefix = prefix;
    }

    @Override
    public MonitoringSubscriptionMetrics subscriptionMetrics(SubscriptionName name) {
        String rateMetric = metricPath(name);
        String timeouts = metricPathTimeouts(name);
        String throughput = metricPathThroughput(name);
        String otherErrors = metricPathOtherErrors(name);
        String codes2xxPath = metricPathHttpStatuses(name, "2xx");
        String codes4xxPath = metricPathHttpStatuses(name, "4xx");
        String codes5xxPath = metricPathHttpStatuses(name, "5xx");
        String batchPath = metricPathBatchRate(name);

        MonitoringMetricsContainer metricsContainer = graphiteClient.readMetrics(codes2xxPath, codes4xxPath, codes5xxPath,
                rateMetric, throughput, timeouts, otherErrors, batchPath);

        return MonitoringSubscriptionMetricsProvider.metricsBuilder()
                .withRate(metricsContainer.metricValue(rateMetric))
                .withTimeouts(metricsContainer.metricValue(timeouts))
                .withThroughput(metricsContainer.metricValue(throughput))
                .withOtherErrors(metricsContainer.metricValue(otherErrors))
                .withCodes2xx(metricsContainer.metricValue(codes2xxPath))
                .withCode4xx(metricsContainer.metricValue(codes4xxPath))
                .withCode5xx(metricsContainer.metricValue(codes5xxPath))
                .withMetricPathBatchRate(metricsContainer.metricValue(batchPath))
                .build();
    }

    @Override
    public MonitoringTopicMetrics topicMetrics(TopicName topicName) {
        String rateMetric = metricPath(TOPIC_RATE_PATTERN, topicName);
        String deliveryRateMetric = metricPath(TOPIC_DELIVERY_RATE_PATTERN, topicName);
        String throughputMetric = metricPath(TOPIC_THROUGHPUT_PATTERN, topicName);

        MonitoringMetricsContainer metrics = graphiteClient.readMetrics(rateMetric, deliveryRateMetric, throughputMetric);
        return MonitoringTopicMetricsProvider.metricsBuilder()
                .withRate(metrics.metricValue(rateMetric))
                .withDeliveryRate(metrics.metricValue(deliveryRateMetric))
                .withThroughput(metrics.metricValue(throughputMetric))
                .build();
    }

    private String metricPath(SubscriptionName name) {
        return String.format(SUBSCRIPTION_RATE_PATTERN, prefix, subscriptionNameToPath(name)
        );
    }

    private String metricPath(String pattern, TopicName topicName) {
        return String.format(pattern, prefix, escapeDots(topicName.getGroupName()),
                escapeDots(topicName.getName()));
    }

    private String metricPathThroughput(SubscriptionName name) {
        return String.format(SUBSCRIPTION_THROUGHPUT_PATTERN, prefix, subscriptionNameToPath(name));
    }

    private String metricPathHttpStatuses(SubscriptionName name, String statusCodeClass) {
        return String.format(SUBSCRIPTION_HTTP_STATUSES_PATTERN, prefix, subscriptionNameToPath(name), statusCodeClass);
    }

    private String metricPathTimeouts(SubscriptionName name) {
        return String.format(SUBSCRIPTION_ERROR_TIMEOUT_PATTERN, prefix, subscriptionNameToPath(name)
        );
    }

    private String metricPathOtherErrors(SubscriptionName name) {
        return String.format(SUBSCRIPTION_ERROR_OTHER_PATTERN, prefix, subscriptionNameToPath(name));
    }

    private String metricPathBatchRate(SubscriptionName name) {
        return String.format(SUBSCRIPTION_BATCH_RATE_PATTERN, prefix, subscriptionNameToPath(name));
    }

    private String subscriptionNameToPath(SubscriptionName name) {
        return String.format(SUBSCRIPTION_PATH,
                escapeDots(name.getTopicName().getGroupName()), name.getTopicName().getName(), escapeDots(name.getName())
        );
    }
}
