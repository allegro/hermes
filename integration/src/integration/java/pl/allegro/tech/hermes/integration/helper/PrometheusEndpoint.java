package pl.allegro.tech.hermes.integration.helper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.List;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.env.EnvironmentAware;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

public class PrometheusEndpoint implements EnvironmentAware {

    private static final String TIMESTAMP = "1396860420";

    private static final String TOPIC_REQUESTS_TOTAL = "hermes_frontend_topic_requests_total";
    private static final String TOPIC_DELIVERED_TOTAL = "hermes_consumers_subscription_delivered_total";
    private static final String TOPIC_THROUGHPUT_TOTAL = "hermes_frontend_topic_throughput_bytes_total";

    private static final String SUBSCRIPTION_DELIVERED = "hermes_consumers_subscription_delivered_total";
    private static final String SUBSCRIPTION_TIMEOUTS = "hermes_consumers_subscription_timeouts_total";
    private static final String SUBSCRIPTION_THROUGHPUT = "hermes_consumers_subscription_throughput_bytes_total";
    private static final String SUBSCRIPTION_OTHER_ERRORS = "hermes_consumers_subscription_other_errors_total";
    private static final String SUBSCRIPTION_BATCHES = "hermes_consumers_subscription_batches_total";
    private static final String SUBSCRIPTION_STATUS_CODES = "hermes_consumers_subscription_http_status_codes_total";

    private static final String TOPIC_QUERY_PATTERN = ".*hermes_frontend_topic_requests_total" +
            ".*hermes_consumers_subscription_delivered_total.*" +
            ".*hermes_frontend_topic_throughput_bytes_total.*GROUP.*TOPIC.*";

    private static final String SUBSCRIPTION_QUERY_PATTERN = ".*hermes_consumers_subscription_delivered_total" +
            ".*hermes_consumers_subscription_timeouts_total" +
            ".*hermes_consumers_subscription_throughput_bytes_total" +
            ".*hermes_consumers_subscription_other_errors_total" +
            ".*hermes_consumers_subscription_batches_total" +
            ".*hermes_consumers_subscription_http_status_codes_total.*";

    private final ObjectMapper objectMapper;
    private final WireMock prometheusListener;

    public PrometheusEndpoint(WireMockServer prometheus) {
        this.prometheusListener = new WireMock("localhost", prometheus.port());
        this.objectMapper = new ObjectMapper();
    }

    public void returnTopicMetrics(String group, String topic, int rate, int deliveryRate) {
        String response = generateTopicsMetricsResponse(rate, deliveryRate);
        String query = TOPIC_QUERY_PATTERN
                .replaceAll("GROUP", group)
                .replaceAll("TOPIC", topic);
        prometheusListener.register(get(urlMatching(query))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    public void returnSubscriptionMetrics(Topic topic, String subscription, int rate) {
        String response = generateSubscriptionResponse(rate);
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

    private String generateTopicsMetricsResponse(int rate, int deliveryRate) {
        return writeToString(
                new PrometheusResponse(
                        "success",
                        new PrometheusResponse.Data(
                                "vector",
                                List.of(
                                        new PrometheusResponse.Result(
                                                new PrometheusResponse.MetricName(TOPIC_REQUESTS_TOTAL),
                                                List.of(TIMESTAMP, String.valueOf(rate))),
                                        new PrometheusResponse.Result(
                                                new PrometheusResponse.MetricName(TOPIC_DELIVERED_TOTAL),
                                                List.of(TIMESTAMP, String.valueOf(deliveryRate))))
                        )));
    }

    private String generateSubscriptionResponse(int rate) {
        return writeToString(
                new PrometheusResponse(
                        "success",
                        new PrometheusResponse.Data(
                                "vector",
                                List.of(
                                        new PrometheusResponse.Result(
                                                new PrometheusResponse.MetricName(SUBSCRIPTION_DELIVERED),
                                                List.of(TIMESTAMP, String.valueOf(rate)))
                                ))));
    }


//    public void returnServerErrorForAllTopics() {
//        graphiteListener.register(get(urlMatching(TOPIC_URL_PATTERN))
//                .willReturn(aResponse()
//                        .withStatus(500)
//                        .withHeader("Content-Type", "application/json")
//                )
//        );
//    }
//
//    public void returnMetric(SubscriptionMetricsStubDefinition metricsStubDefinition) {
//        graphiteListener.register(get(urlMatching(metricsStubDefinition.toUrlPattern()))
//                .willReturn(aResponse()
//                        .withStatus(200)
//                        .withHeader("Content-Type", "application/json")
//                        .withBody(metricsStubDefinition.toBody())));
//    }
//
//    public void returnMetricWithDelay(SubscriptionMetricsStubDefinition metricsStubDefinition, int responseDelayInMs) {
//        graphiteListener.register(get(urlMatching(metricsStubDefinition.toUrlPattern()))
//                .willReturn(aResponse()
//                        .withFixedDelay(responseDelayInMs)
//                        .withStatus(200)
//                        .withHeader("Content-Type", "application/json")
//                        .withBody(metricsStubDefinition.toBody())));
//    }
//
//    private static class SubscriptionMetricsStubDefinition {
//        private final String subscription;
//        private final List<GraphiteStubResponse> responseBody;
//
//        private SubscriptionMetricsStubDefinition(String subscription, List<GraphiteStubResponse> responseBody) {
//            this.subscription = subscription;
//            this.responseBody = responseBody;
//        }
//
//        private String toUrlPattern() {
//            return "/.*sumSeries%28stats.tech.hermes\\.consumer\\.%2A\\.meter\\." + subscription + "\\.m1_rate%29.*";
//        }
//
//        private String toBody() {
//            try {
//                return new ObjectMapper().writeValueAsString(responseBody);
//            } catch (JsonProcessingException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }
//
//    public static class SubscriptionMetricsStubDefinitionBuilder {
//        private final String subscription;
//        private final List<GraphiteStubResponse> response = new ArrayList<>();
//
//        private SubscriptionMetricsStubDefinitionBuilder(String subscription) {
//            this.subscription = subscription;
//        }
//
//        public SubscriptionMetricsStubDefinitionBuilder withRate(int rate) {
//            String target = "sumSeries(stats.tech.hermes.consumer.*.meter." + subscription + ".m1_rate)";
//            response.add(new GraphiteStubResponse(target, dataPointOf(rate)));
//            return this;
//        }
//
//        public SubscriptionMetricsStubDefinitionBuilder withThroughput(int rate) {
//            String target = "sumSeries(stats.tech.hermes.consumer.*.throughput." + subscription + ".m1_rate)";
//            response.add(new GraphiteStubResponse(target, dataPointOf(rate)));
//            return this;
//        }
//
//        public SubscriptionMetricsStubDefinitionBuilder withStatusRate(int httpStatus, int rate) {
//            String statusFamily = httpStatusFamily(httpStatus);
//            String target = "sumSeries(stats.tech.hermes.consumer.*.status." + subscription + "." + statusFamily + ".m1_rate)";
//            response.add(new GraphiteStubResponse(target, dataPointOf(rate)));
//            return this;
//        }
//
//        public SubscriptionMetricsStubDefinition build() {
//            return new SubscriptionMetricsStubDefinition(subscription, response);
//        }
//
//        private String httpStatusFamily(int statusCode) {
//            return format("%dxx", statusCode / 100);
//        }
//
//        private static List<List<Object>> dataPointOf(int rate) {
//            return singletonList(asList(rate, TIMESTAMP));
//        }
//    }
//
//    public static SubscriptionMetricsStubDefinitionBuilder subscriptionMetricsStub(String subscription) {
//        return new SubscriptionMetricsStubDefinitionBuilder(subscription);
//    }

    String writeToString(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    record PrometheusResponse(@JsonProperty("status") String status,
                              @JsonProperty("data") Data data) {

        boolean isSuccess() {
            return status.equals("success") && data.isVector();
        }

        record Data(@JsonProperty("resultType") String resultType,
                    @JsonProperty("result") List<Result> results) {
            boolean isVector() {
                return resultType.equals("vector");
            }
        }

        record Result(
                @JsonProperty("metric") MetricName metricName,
                @JsonProperty("value") List<String> values) {
        }

        record MetricName(
                @JsonProperty(value = "__name__") String name) {
        }
    }
}
