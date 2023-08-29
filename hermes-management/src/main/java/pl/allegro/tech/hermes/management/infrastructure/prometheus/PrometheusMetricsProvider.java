package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringSubscriptionMetricsProvider;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringTopicMetricsProvider;

import java.util.List;

public class PrometheusMetricsProvider implements MonitoringSubscriptionMetricsProvider, MonitoringTopicMetricsProvider {

    private static final String SUBSCRIPTION_DELIVERED = "subscription_delivered_total";
    private static final String SUBSCRIPTION_TIMEOUTS = "subscription_timeouts_total";
    private static final String SUBSCRIPTION_THROUGHPUT = "subscription_throughput_bytes_total";
    private static final String SUBSCRIPTION_OTHER_ERRORS = "subscription_other_errors_total";
    private static final String SUBSCRIPTION_BATCHES = "subscription_batches_total";
    private static final String SUBSCRIPTION_STATUS_CODES = "subscription_http_status_codes_total";
    private static final String SUBSCRIPTION_STATUS_CODES_2XX = SUBSCRIPTION_STATUS_CODES + "_2xx";
    private static final String SUBSCRIPTION_STATUS_CODES_4XX = SUBSCRIPTION_STATUS_CODES + "_4xx";
    private static final String SUBSCRIPTION_STATUS_CODES_5XX = SUBSCRIPTION_STATUS_CODES + "_5xx";

    private static final String TOPIC_RATE = "topic_requests_total";
    private static final String TOPIC_DELIVERY_RATE = "subscription_delivered_total";
    private static final String TOPIC_THROUGHPUT_RATE = "topic_throughput_bytes_total";

    private final String consumersMetricsPrefix;
    private final String frontendMetricsPrefix;
    private final String subscriptionMetricsToQuery;
    private final String topicMetricsToQuery;
    private final PrometheusClient prometheusClient;

    public PrometheusMetricsProvider(PrometheusClient prometheusClient, String consumersMetricsPrefix,
                                     String frontendMetricsPrefix) {
        this.prometheusClient = prometheusClient;
        this.consumersMetricsPrefix = consumersMetricsPrefix.isEmpty() ? "" : consumersMetricsPrefix + "_";
        this.frontendMetricsPrefix = frontendMetricsPrefix.isEmpty() ? "" : frontendMetricsPrefix + "_";
        this.subscriptionMetricsToQuery = Stream.of(SUBSCRIPTION_DELIVERED, SUBSCRIPTION_TIMEOUTS,
                        SUBSCRIPTION_THROUGHPUT, SUBSCRIPTION_OTHER_ERRORS, SUBSCRIPTION_BATCHES,
                        SUBSCRIPTION_STATUS_CODES)
                .map(this::consumerMetricName)
                .collect(Collectors.joining("|"));
        this.topicMetricsToQuery = String.join("|", List.of(
                frontendMetricName(TOPIC_RATE),
                consumerMetricName(TOPIC_DELIVERY_RATE),
                frontendMetricName(TOPIC_THROUGHPUT_RATE)
        ));
    }

    @Override
    public MonitoringSubscriptionMetrics subscriptionMetrics(SubscriptionName subscriptionName) {
        /*
        The query is based on MetricsQL, available only in VictoriaMetrics
        https://docs.victoriametrics.com/MetricsQL.html. Basic PromQL does not support `keep_metric_names` param.
         */
        String queryFormat = "sum by (__name__,group,topic,subscription,status_code)"
                + "(irate({__name__=~'%s',group='%s',topic='%s',subscription='%s'}[1m]) keep_metric_names)";
        String query = String.format(queryFormat, subscriptionMetricsToQuery, subscriptionName.getTopicName().getGroupName(),
                subscriptionName.getTopicName().getName(), subscriptionName.getName());
        MonitoringMetricsContainer prometheusMetricsContainer = prometheusClient.readMetrics(query);
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
                .build();
    }

    @Override
    public MonitoringTopicMetrics topicMetrics(TopicName topicName) {
        /*
        The query is based on MetricsQL, available only in VictoriaMetrics
        https://docs.victoriametrics.com/MetricsQL.html. Basic PromQL does not support `keep_metric_names` param.
         */
        String queryFormat = "sum by (__name__, group, topic) (irate({__name__=~'%s', group='%s', "
                + "topic='%s'}[1m]) keep_metric_names)";
        String query = String.format(queryFormat, topicMetricsToQuery, topicName.getGroupName(), topicName.getName());
        MonitoringMetricsContainer prometheusMetricsContainer = prometheusClient.readMetrics(query);
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