package pl.allegro.tech.hermes.integrationtests.prometheus;

import java.util.List;
import pl.allegro.tech.hermes.api.TopicName;

public record TopicMetrics(TopicName name, int rate, int deliveryRate, int throughput) {

  private static final String TIMESTAMP = "1396860420";

  public static TopicMetricsBuilder topicMetrics(TopicName name) {
    return new TopicMetricsBuilder(name);
  }

  PrometheusResponse toPrometheusRequestsResponse() {
    return new PrometheusResponse(
        "success",
        new PrometheusResponse.Data(
            "vector",
            List.of(new PrometheusResponse.Result(List.of(TIMESTAMP, String.valueOf(rate))))));
  }

  PrometheusResponse toDeliveredResponse() {
    return new PrometheusResponse(
        "success",
        new PrometheusResponse.Data(
            "vector",
            List.of(
                new PrometheusResponse.Result(List.of(TIMESTAMP, String.valueOf(deliveryRate))))));
  }

  PrometheusResponse toPrometheusThroughputResponse() {
    return new PrometheusResponse(
        "success",
        new PrometheusResponse.Data(
            "vector",
            List.of(
                new PrometheusResponse.Result(List.of(TIMESTAMP, String.valueOf(throughput))))));
  }

  public static class TopicMetricsBuilder {
    private final TopicName name;
    private int rate = 0;
    private int deliveryRate = 0;
    private int throughput = 0;

    private TopicMetricsBuilder(TopicName name) {
      this.name = name;
    }

    public TopicMetricsBuilder withRate(int rate) {
      this.rate = rate;
      return this;
    }

    public TopicMetricsBuilder withDeliveryRate(int deliveryRate) {
      this.deliveryRate = deliveryRate;
      return this;
    }

    public TopicMetricsBuilder withThroughput(int throughput) {
      this.throughput = throughput;
      return this;
    }

    public TopicMetrics build() {
      return new TopicMetrics(name, rate, deliveryRate, throughput);
    }
  }
}
