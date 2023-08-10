package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringSubscriptionMetricsProvider;

public class PrometheusMetricsProvider implements MonitoringSubscriptionMetricsProvider {

    private final PrometheusClient prometheusClient;

    public PrometheusMetricsProvider(PrometheusClient prometheusClient) {
        this.prometheusClient = prometheusClient;
    }

    @Override
    public MonitoringSubscriptionMetrics provide(SubscriptionName subscriptionName) {
        throw new UnsupportedOperationException("Not implemented");
    }


}
