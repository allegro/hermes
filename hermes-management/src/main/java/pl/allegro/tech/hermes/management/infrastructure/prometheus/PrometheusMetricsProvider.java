package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringSubscriptionMetricsProvider;

import java.util.stream.Collectors;
import java.util.stream.Stream;


public class PrometheusMetricsProvider implements MonitoringSubscriptionMetricsProvider {

    private final String prefix;
    private final String metricsToQuery;

    private final PrometheusClient prometheusClient;

    public PrometheusMetricsProvider(PrometheusClient prometheusClient, String prefix) {
        this.prometheusClient = prometheusClient;
        this.prefix = prefix;
        this.metricsToQuery = Stream.of(
                        "subscription_delivered",
                        "subscription_timeouts",
                        "subscription_throughput",
                        "subscription_other_errors",
                        "subscription_batches")
                .map(this::consumerMetricName)
                .collect(Collectors.joining("|"));
    }

    @Override
    public MonitoringSubscriptionMetrics provide(SubscriptionName subscriptionName) {
        String query = "sum({__name__=~'%s', group='%s', topic='%s', subscription='%s'}) by (__name__)";
        String.format(query, metricsToQuery, subscriptionName.getTopicName().getGroupName(),
                subscriptionName.getTopicName().getName(), subscriptionName.getName());

    }

    private String consumerMetricName(String name) {
        return prefix + name;
    }

}
