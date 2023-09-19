package pl.allegro.tech.hermes.integration.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.env.EnvironmentAware;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

public class PrometheusEndpoint {

    private static final String TIMESTAMP = "1396860420";

    private static final String TOPIC_REQUESTS_TOTAL = "hermes_frontend_topic_requests_total";
    private static final String TOPIC_DELIVERED_TOTAL = "hermes_consumers_subscription_delivered_total";
    private static final String TOPIC_THROUGHPUT_TOTAL = "hermes_frontend_topic_throughput_bytes_total";

    private static final String SUBSCRIPTION_DELIVERED = "hermes_consumers_subscription_delivered_total";
    private static final String SUBSCRIPTION_THROUGHPUT = "hermes_consumers_subscription_throughput_bytes_total";
    private static final String SUBSCRIPTION_STATUS_CODES = "hermes_consumers_subscription_http_status_codes_total";

    private static final String TOPIC_QUERY_PATTERN = ".*hermes_frontend_topic_requests_total"
            + ".*hermes_consumers_subscription_delivered_total.*"
            + ".*hermes_frontend_topic_throughput_bytes_total.*GROUP.*TOPIC.*";

    private static final String SUBSCRIPTION_QUERY_PATTERN = ".*hermes_consumers_subscription_delivered_total"
            + ".*hermes_consumers_subscription_timeouts_total"
            + ".*hermes_consumers_subscription_throughput_bytes_total"
            + ".*hermes_consumers_subscription_other_errors_total"
            + ".*hermes_consumers_subscription_batches_total"
            + ".*hermes_consumers_subscription_http_status_codes_total.*GROUP.*TOPIC.*SUBSCRIPTION.*";

    private final ObjectMapper objectMapper;
    private final WireMock prometheusListener;

    public PrometheusEndpoint(WireMockServer prometheus) {
        this.prometheusListener = new WireMock("localhost", prometheus.port());
        this.objectMapper = new ObjectMapper();
    }

    public void returnTopicMetrics(Topic topic, PrometheusTopicResponse topicStub) {
        String response = generateTopicsMetricsResponse(topicStub.topicRate, topicStub.deliveredRate, topicStub.throughput);
        String query = TOPIC_QUERY_PATTERN
                .replaceAll("GROUP", topic.getName().getGroupName())
                .replaceAll("TOPIC", topic.getName().getName());
        prometheusListener.register(get(urlMatching(query))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    public void returnSubscriptionMetrics(Topic topic, String subscription, PrometheusSubscriptionResponse stub) {
        String response = generateSubscriptionResponse(stub);
        String query = SUBSCRIPTION_QUERY_PATTERN
                .replaceAll("GROUP", topic.getName().getGroupName())
                .replaceAll("TOPIC", topic.getName().getName())
                .replaceAll("SUBSCRIPTION", subscription);
        prometheusListener.register(get(urlMatching(query))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    public void returnSubscriptionMetricsWithDelay(Topic topic, String subscription, PrometheusSubscriptionResponse stub,
                                                   int prometheusDelay) {
        String response = generateSubscriptionResponse(stub);
        String query = SUBSCRIPTION_QUERY_PATTERN
                .replaceAll("GROUP", topic.getName().getGroupName())
                .replaceAll("TOPIC", topic.getName().getName())
                .replaceAll("SUBSCRIPTION", subscription);
        prometheusListener.register(get(urlMatching(query))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(prometheusDelay)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    public void returnServerErrorForAllTopics() {
        String query = TOPIC_QUERY_PATTERN
                .replaceAll("GROUP", "")
                .replaceAll("TOPIC", "");
        prometheusListener.register(get(urlMatching(query))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")));
    }

    private String generateTopicsMetricsResponse(int rate, int deliveryRate, int throughput) {
        return writeToString(
                new PrometheusResponse(
                        "success",
                        new PrometheusResponse.Data(
                                "vector",
                                List.of(
                                        new PrometheusResponse.Result(
                                                new PrometheusResponse.MetricName(TOPIC_REQUESTS_TOTAL, null),
                                                List.of(TIMESTAMP, String.valueOf(rate))),
                                        new PrometheusResponse.Result(
                                                new PrometheusResponse.MetricName(TOPIC_DELIVERED_TOTAL, null),
                                                List.of(TIMESTAMP, String.valueOf(deliveryRate))),
                                        new PrometheusResponse.Result(
                                                new PrometheusResponse.MetricName(TOPIC_THROUGHPUT_TOTAL, null),
                                                List.of(TIMESTAMP, String.valueOf(throughput))))
                        )));
    }

    private String generateSubscriptionResponse(PrometheusSubscriptionResponse stub) {
        List<PrometheusResponse.Result> results = new ArrayList<>();
        results.add(
                new PrometheusResponse.Result(
                        new PrometheusResponse.MetricName(SUBSCRIPTION_DELIVERED, null),
                        List.of(TIMESTAMP, String.valueOf(stub.rate)))
        );
        results.add(
                new PrometheusResponse.Result(
                        new PrometheusResponse.MetricName(SUBSCRIPTION_THROUGHPUT, null),
                        List.of(TIMESTAMP, String.valueOf(stub.throughput))
                )
        );
        stub.statusCodes().forEach(s -> results.add(
                new PrometheusResponse.Result(
                        new PrometheusResponse.MetricName(SUBSCRIPTION_STATUS_CODES, s.code()),
                        List.of(TIMESTAMP, String.valueOf(s.rate)))));
        PrometheusResponse response = new PrometheusResponse(
                "success", new PrometheusResponse.Data("vector", results));
        return writeToString(response);
    }

    String writeToString(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public record PrometheusTopicResponse(int topicRate, int deliveredRate, int throughput) {
    }

    public record PrometheusSubscriptionResponse(int rate, int throughput, List<SubscriptionStatusCode> statusCodes) {
    }

    public record SubscriptionStatusCode(String code, int rate) {
    }

    public static class PrometheusSubscriptionResponseBuilder {
        private int rate = 0;
        private int throughput = 0;
        private final List<SubscriptionStatusCode> statusCodes = new ArrayList<>();

        private PrometheusSubscriptionResponseBuilder() {
        }

        public static PrometheusSubscriptionResponseBuilder builder() {
            return new PrometheusSubscriptionResponseBuilder();
        }

        public PrometheusSubscriptionResponseBuilder withRate(int rate) {
            this.rate = rate;
            return this;
        }

        public PrometheusSubscriptionResponseBuilder withThroughput(int throughput) {
            this.throughput = throughput;
            return this;
        }

        public PrometheusSubscriptionResponseBuilder withRatedStatusCode(String statusCode, int rate) {
            this.statusCodes.add(new SubscriptionStatusCode(statusCode, rate));
            return this;
        }

        public PrometheusSubscriptionResponse build() {
            return new PrometheusSubscriptionResponse(rate, throughput, statusCodes);
        }
    }

    record PrometheusResponse(@JsonProperty("status") String status,
                              @JsonProperty("data") Data data) {

        record Data(@JsonProperty("resultType") String resultType,
                    @JsonProperty("result") List<Result> results) {
        }

        record Result(
                @JsonProperty("metric") MetricName metricName,
                @JsonProperty("value") List<String> values) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        record MetricName(
                @JsonProperty(value = "__name__") String name,
                @JsonProperty(value = "status_code") String statusCode
        ) {
        }
    }
}
