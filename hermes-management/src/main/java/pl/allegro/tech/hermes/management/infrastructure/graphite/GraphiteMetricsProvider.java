package pl.allegro.tech.hermes.management.infrastructure.graphite;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringSubscriptionMetricsProvider;
import pl.allegro.tech.hermes.management.stub.MetricsPaths;

import static pl.allegro.tech.hermes.common.metric.HermesMetrics.escapeDots;

public class GraphiteMetricsProvider implements MonitoringSubscriptionMetricsProvider {

    private static final String SUBSCRIPTION_PATH = "%s.%s.%s";

    private static final String SUBSCRIPTION_RATE_PATTERN = "sumSeries(%s.consumer.*.meter.%s.m1_rate)";
    private static final String SUBSCRIPTION_THROUGHPUT_PATTERN = "sumSeries(%s.consumer.*.throughput.%s.m1_rate)";
    private static final String SUBSCRIPTION_HTTP_STATUSES_PATTERN = "sumSeries(%s.consumer.*.status.%s.%s.m1_rate)";
    private static final String SUBSCRIPTION_ERROR_TIMEOUT_PATTERN = "sumSeries(%s.consumer.*.status.%s.errors.timeout.m1_rate)";
    private static final String SUBSCRIPTION_ERROR_OTHER_PATTERN = "sumSeries(%s.consumer.*.status.%s.errors.other.m1_rate)";
    private static final String SUBSCRIPTION_BATCH_RATE_PATTERN = "sumSeries(%s.consumer.*.meter.%s.batch.m1_rate)";

    private final GraphiteClient graphiteClient;
    private final MetricsPaths metricsPaths;

    public GraphiteMetricsProvider(GraphiteClient graphiteClient, MetricsPaths metricsPaths) {
        this.graphiteClient = graphiteClient;
        this.metricsPaths = metricsPaths;
    }

    @Override
    public MonitoringSubscriptionMetrics provide(SubscriptionName name) {
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


    private String metricPath(SubscriptionName name) {
        return String.format(SUBSCRIPTION_RATE_PATTERN,
                metricsPaths.prefix(), subscriptionNameToPath(name)
        );
    }

    private String metricPathThroughput(SubscriptionName name) {
        return String.format(SUBSCRIPTION_THROUGHPUT_PATTERN,
                metricsPaths.prefix(), subscriptionNameToPath(name)
        );
    }

    private String metricPathHttpStatuses(SubscriptionName name, String statusCodeClass) {
        return String.format(SUBSCRIPTION_HTTP_STATUSES_PATTERN,
                metricsPaths.prefix(), subscriptionNameToPath(name), statusCodeClass
        );
    }

    private String metricPathTimeouts(SubscriptionName name) {
        return String.format(SUBSCRIPTION_ERROR_TIMEOUT_PATTERN,
                metricsPaths.prefix(), subscriptionNameToPath(name)
        );
    }

    private String metricPathOtherErrors(SubscriptionName name) {
        return String.format(SUBSCRIPTION_ERROR_OTHER_PATTERN,
                metricsPaths.prefix(), subscriptionNameToPath(name)
        );
    }

    private String metricPathBatchRate(SubscriptionName name) {
        return String.format(SUBSCRIPTION_BATCH_RATE_PATTERN,
                metricsPaths.prefix(), subscriptionNameToPath(name)
        );
    }

    private String subscriptionNameToPath(SubscriptionName name) {
        return String.format(SUBSCRIPTION_PATH,
                escapeDots(name.getTopicName().getGroupName()), name.getTopicName().getName(), escapeDots(name.getName())
        );
    }
}
