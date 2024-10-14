package pl.allegro.tech.hermes.management.infrastructure.metrics;

import pl.allegro.tech.hermes.api.MetricDecimalValue;
import pl.allegro.tech.hermes.api.SubscriptionName;

public interface MonitoringSubscriptionMetricsProvider {
  MonitoringSubscriptionMetrics subscriptionMetrics(SubscriptionName subscriptionName);

  record MonitoringSubscriptionMetrics(
      MetricDecimalValue rate,
      MetricDecimalValue timeouts,
      MetricDecimalValue throughput,
      MetricDecimalValue otherErrors,
      MetricDecimalValue codes2xx,
      MetricDecimalValue code4xx,
      MetricDecimalValue code5xx,
      MetricDecimalValue retries,
      MetricDecimalValue metricPathBatchRate) {}

  static MetricsBuilder metricsBuilder() {
    return new MetricsBuilder();
  }

  class MetricsBuilder {
    private MetricDecimalValue rate;
    private MetricDecimalValue timeouts;
    private MetricDecimalValue throughput;
    private MetricDecimalValue otherErrors;
    private MetricDecimalValue codes2xx;
    private MetricDecimalValue code4xx;
    private MetricDecimalValue code5xx;
    private MetricDecimalValue retries;
    private MetricDecimalValue metricPathBatchRate;

    public MetricsBuilder withRate(MetricDecimalValue rate) {
      this.rate = rate;
      return this;
    }

    public MetricsBuilder withTimeouts(MetricDecimalValue timeouts) {
      this.timeouts = timeouts;
      return this;
    }

    public MetricsBuilder withThroughput(MetricDecimalValue throughput) {
      this.throughput = throughput;
      return this;
    }

    public MetricsBuilder withOtherErrors(MetricDecimalValue otherErrors) {
      this.otherErrors = otherErrors;
      return this;
    }

    public MetricsBuilder withCodes2xx(MetricDecimalValue codes2xx) {
      this.codes2xx = codes2xx;
      return this;
    }

    public MetricsBuilder withCode4xx(MetricDecimalValue code4xx) {
      this.code4xx = code4xx;
      return this;
    }

    public MetricsBuilder withCode5xx(MetricDecimalValue code5xx) {
      this.code5xx = code5xx;
      return this;
    }

    public MetricsBuilder withRetries(MetricDecimalValue retries) {
      this.retries = retries;
      return this;
    }

    public MetricsBuilder withMetricPathBatchRate(MetricDecimalValue metricPathBatchRate) {
      this.metricPathBatchRate = metricPathBatchRate;
      return this;
    }

    public MonitoringSubscriptionMetrics build() {
      return new MonitoringSubscriptionMetrics(
          rate,
          timeouts,
          throughput,
          otherErrors,
          codes2xx,
          code4xx,
          code5xx,
          retries,
          metricPathBatchRate);
    }
  }
}
