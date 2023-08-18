package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringSubscriptionMetricsProvider;

import java.util.stream.Collectors;
import java.util.stream.Stream;


public class PrometheusMetricsProvider implements MonitoringSubscriptionMetricsProvider {

    private static final String DELIVERED = "subscription_delivered_total";
    private static final String TIMEOUTS = "subscription_timeouts_total";
    private static final String THROUGHPUT = "subscription_throughput_bytes_total";
    private static final String OTHER_ERRORS = "subscription_other_errors_total";
    private static final String BATCHES = "subscription_batches_total";
    private static final String STATUS_CODES = "subscription_http_status_codes_total";
    private static final String STATUS_CODES_2XX = STATUS_CODES + "_2xx";
    private static final String STATUS_CODES_4XX = STATUS_CODES + "_4xx";
    private static final String STATUS_CODES_5XX = STATUS_CODES + "_5xx";

    private final String prefix;
    private final String metricsToQuery;
    private final PrometheusClient prometheusClient;

    public PrometheusMetricsProvider(PrometheusClient prometheusClient, String prefix) {
        this.prometheusClient = prometheusClient;
        this.prefix = prefix;
        this.metricsToQuery = Stream.of(DELIVERED, TIMEOUTS, THROUGHPUT, OTHER_ERRORS, BATCHES, STATUS_CODES)
                .map(this::consumerMetricName)
                .collect(Collectors.joining("|"));
    }

    @Override
    public MonitoringSubscriptionMetrics provide(SubscriptionName subscriptionName) {
        String rawQuery = ("sum by (__name__,group,topic,subscription,status_code)" +
                "(irate({__name__=~'%s',group='%s',topic='%s',subscription='%s'}[1m]) keep_metric_names)");
        String query = String.format(rawQuery, metricsToQuery, subscriptionName.getTopicName().getGroupName(),
                subscriptionName.getTopicName().getName(), subscriptionName.getName());
        MonitoringMetricsContainer prometheusMetricsContainer = prometheusClient.readMetrics(query);
        return MonitoringSubscriptionMetricsProvider
                .metricsBuilder()
                .withRate(prometheusMetricsContainer.metricValue(consumerMetricName(DELIVERED)))
                .withTimeouts(prometheusMetricsContainer.metricValue(consumerMetricName(TIMEOUTS)))
                .withThroughput(prometheusMetricsContainer.metricValue(consumerMetricName(THROUGHPUT)))
                .withOtherErrors(prometheusMetricsContainer.metricValue(consumerMetricName(OTHER_ERRORS)))
                .withMetricPathBatchRate(prometheusMetricsContainer.metricValue(consumerMetricName(BATCHES)))
                .withCodes2xx(prometheusMetricsContainer.metricValue(consumerMetricName(STATUS_CODES_2XX)))
                .withCode4xx(prometheusMetricsContainer.metricValue(consumerMetricName(STATUS_CODES_4XX)))
                .withCode5xx(prometheusMetricsContainer.metricValue(consumerMetricName(STATUS_CODES_5XX)))
                .build();
    }

    private String consumerMetricName(String name) {
        return prefix + name;
    }
}