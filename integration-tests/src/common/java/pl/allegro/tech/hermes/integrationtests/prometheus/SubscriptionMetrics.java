package pl.allegro.tech.hermes.integrationtests.prometheus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pl.allegro.tech.hermes.api.SubscriptionName;

public record SubscriptionMetrics(
    SubscriptionName name,
    int rate,
    int throughput,
    Map<String, Integer> ratePerStatusCode,
    Map<String, String> messageProcessingTime) {

  private static final String TIMESTAMP = "1396860420";

  public static SubscriptionMetricsBuilder subscriptionMetrics(SubscriptionName name) {
    return new SubscriptionMetricsBuilder(name);
  }

  PrometheusResponse toPrometheusRateResponse() {
    List<PrometheusResponse.Result> results = new ArrayList<>();
    results.add(new PrometheusResponse.Result(List.of(TIMESTAMP, String.valueOf(rate))));
    return new PrometheusResponse("success", new PrometheusResponse.Data("vector", results));
  }

  PrometheusResponse toPrometheusThroughputResponse() {
    List<PrometheusResponse.Result> results = new ArrayList<>();
    results.add(new PrometheusResponse.Result(List.of(TIMESTAMP, String.valueOf(throughput))));
    return new PrometheusResponse("success", new PrometheusResponse.Data("vector", results));
  }

  PrometheusResponse toPrometheusStatusCodesResponse() {
    List<PrometheusResponse.Result> results = new ArrayList<>();
    ratePerStatusCode.forEach(
        (code, rate) ->
            results.add(new PrometheusResponse.Result(List.of(TIMESTAMP, String.valueOf(rate)))));
    return new PrometheusResponse("success", new PrometheusResponse.Data("vector", results));
  }

  PrometheusResponse toPrometheusMessageProcessingTimeHistogramResponse() {
    List<PrometheusResponse.Result> results = new ArrayList<>();
    messageProcessingTime.forEach(
        (bucket, count) ->
            results.add(
                new PrometheusResponse.Result(
                    new PrometheusResponse.Metric(bucket), List.of(TIMESTAMP, count))));
    return new PrometheusResponse("success", new PrometheusResponse.Data("vector", results));
  }

  PrometheusResponse toPrometheusDefaultResponse() {
    List<PrometheusResponse.Result> results = new ArrayList<>();
    ratePerStatusCode.forEach(
        (code, rate) -> results.add(new PrometheusResponse.Result(List.of(TIMESTAMP, "0.0"))));
    return new PrometheusResponse("success", new PrometheusResponse.Data("vector", results));
  }

  public static class SubscriptionMetricsBuilder {
    private final SubscriptionName name;
    private int rate = 0;
    private int throughput = 0;
    private final Map<String, Integer> ratePerStatusCode = new HashMap<>();
    private final Map<String, String> messageProcessingTime = new HashMap<>();

    private SubscriptionMetricsBuilder(SubscriptionName name) {
      this.name = name;
    }

    public SubscriptionMetricsBuilder withRate(int rate) {
      this.rate = rate;
      return this;
    }

    public SubscriptionMetricsBuilder with500Rate(int rate) {
      ratePerStatusCode.put("500", rate);
      return this;
    }

    public SubscriptionMetricsBuilder withThroughput(int throughput) {
      this.throughput = throughput;
      return this;
    }

    public SubscriptionMetricsBuilder withMessageProcessingTime(String bucket, String count) {
      messageProcessingTime.put(bucket, count);
      return this;
    }

    public SubscriptionMetrics build() {
      return new SubscriptionMetrics(
          name, rate, throughput, ratePerStatusCode, messageProcessingTime);
    }
  }
}
