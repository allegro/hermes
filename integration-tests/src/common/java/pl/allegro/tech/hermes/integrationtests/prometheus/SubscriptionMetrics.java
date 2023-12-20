package pl.allegro.tech.hermes.integrationtests.prometheus;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SubscriptionMetrics(SubscriptionName name, int rate, int throughput, Map<String, Integer> ratePerStatusCode) {

    private static final String TIMESTAMP = "1396860420";
    private static final String SUBSCRIPTION_DELIVERED = "hermes_consumers_subscription_delivered_total";
    private static final String SUBSCRIPTION_THROUGHPUT = "hermes_consumers_subscription_throughput_bytes_total";
    private static final String SUBSCRIPTION_STATUS_CODES = "hermes_consumers_subscription_http_status_codes_total";

    public static SubscriptionMetricsBuilder subscriptionMetrics(SubscriptionName name) {
        return new SubscriptionMetricsBuilder(name);
    }

    PrometheusResponse toPrometheusResponse() {
        List<PrometheusResponse.Result> results = new ArrayList<>();
        results.add(
                new PrometheusResponse.Result(
                        new PrometheusResponse.MetricName(SUBSCRIPTION_DELIVERED, null),
                        List.of(TIMESTAMP, String.valueOf(rate)))
        );
        results.add(
                new PrometheusResponse.Result(
                        new PrometheusResponse.MetricName(SUBSCRIPTION_THROUGHPUT, null),
                        List.of(TIMESTAMP, String.valueOf(throughput))
                )
        );
        ratePerStatusCode.forEach((code, rate) -> results.add(
                        new PrometheusResponse.Result(
                                new PrometheusResponse.MetricName(SUBSCRIPTION_STATUS_CODES, code),
                                List.of(TIMESTAMP, String.valueOf(rate))
                        )
                )
        );
        return new PrometheusResponse("success", new PrometheusResponse.Data("vector", results));
    }

    public static class SubscriptionMetricsBuilder {
        private final SubscriptionName name;
        private int rate = 0;
        private int throughput = 0;
        private final Map<String, Integer> ratePerStatusCode = new HashMap<>();

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

        public SubscriptionMetrics build() {
            return new SubscriptionMetrics(name, rate, throughput, ratePerStatusCode);
        }
    }
}
