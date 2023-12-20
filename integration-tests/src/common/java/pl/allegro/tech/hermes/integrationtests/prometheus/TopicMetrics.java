package pl.allegro.tech.hermes.integrationtests.prometheus;

import pl.allegro.tech.hermes.api.TopicName;

import java.util.List;

public record TopicMetrics(TopicName name, int rate, int deliveryRate, int throughput) {

    private static final String TIMESTAMP = "1396860420";
    private static final String TOPIC_REQUESTS_TOTAL = "hermes_frontend_topic_requests_total";
    private static final String TOPIC_DELIVERED_TOTAL = "hermes_consumers_subscription_delivered_total";
    private static final String TOPIC_THROUGHPUT_TOTAL = "hermes_frontend_topic_throughput_bytes_total";

    public static TopicMetricsBuilder topicMetrics(TopicName name) {
        return new TopicMetricsBuilder(name);
    }

    PrometheusResponse toPrometheusResponse() {
        return new PrometheusResponse(
                "success",
                new PrometheusResponse.Data(
                        "vector",
                        List.of(
                                new PrometheusResponse.Result(
                                        new PrometheusResponse.MetricName(TOPIC_REQUESTS_TOTAL, null),
                                        List.of(TIMESTAMP, String.valueOf(rate))
                                ),
                                new PrometheusResponse.Result(
                                        new PrometheusResponse.MetricName(TOPIC_DELIVERED_TOTAL, null),
                                        List.of(TIMESTAMP, String.valueOf(deliveryRate))
                                ),
                                new PrometheusResponse.Result(
                                        new PrometheusResponse.MetricName(TOPIC_THROUGHPUT_TOTAL, null),
                                        List.of(TIMESTAMP, String.valueOf(throughput))
                                )
                        )
                )
        );
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
