package pl.allegro.tech.hermes.management.infrastructure.metrics;

import pl.allegro.tech.hermes.api.MetricDecimalValue;
import pl.allegro.tech.hermes.api.TopicName;

public interface MonitoringTopicMetricsProvider {
  MonitoringTopicMetrics topicMetrics(TopicName topicName);

  record MonitoringTopicMetrics(
      MetricDecimalValue rate, MetricDecimalValue deliveryRate, MetricDecimalValue throughput) {}

  static MetricsBuilder metricsBuilder() {
    return new MetricsBuilder();
  }

  class MetricsBuilder {
    private MetricDecimalValue rate;
    private MetricDecimalValue deliveryRate;
    private MetricDecimalValue throughput;

    public MetricsBuilder withRate(MetricDecimalValue rate) {
      this.rate = rate;
      return this;
    }

    public MetricsBuilder withDeliveryRate(MetricDecimalValue deliveryRate) {
      this.deliveryRate = deliveryRate;
      return this;
    }

    public MetricsBuilder withThroughput(MetricDecimalValue throughput) {
      this.throughput = throughput;
      return this;
    }

    public MonitoringTopicMetrics build() {
      return new MonitoringTopicMetrics(rate, deliveryRate, throughput);
    }
  }
}
