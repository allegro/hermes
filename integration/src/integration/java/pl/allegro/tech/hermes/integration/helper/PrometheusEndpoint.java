package pl.allegro.tech.hermes.integration.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import pl.allegro.tech.hermes.integration.env.EnvironmentAware;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class PrometheusEndpoint implements EnvironmentAware {

    private static final String TIMESTAMP = "1396860420";

    private static final String TOPIC_RESPONSE
            = """
            [
              {
                "metric": {
                  "__name__": "hermes_consumers_subscription_delivered_total",
                  "group": "pl.allegro.product.index.v4",
                  "topic": "productRemoved"
                },
                "value": [
                  1692625025.991,
                  "0"
                ],
                "group": 1
              },
              {
                "metric": {
                  "__name__": "hermes_frontend_topic_requests_total",
                  "group": "pl.allegro.product.index.v4",
                  "topic": "productRemoved"
                },
                "value": [
                  1692625025.991,
                  "0"
                ],
                "group": 1
              },
              {
                "metric": {
                  "__name__": "hermes_frontend_topic_throughput_bytes_total",
                  "group": "pl.allegro.product.index.v4",
                  "topic": "productRemoved"
                },
                "value": [
                  1692625025.991,
                  "0"
                ],
                "group": 1
              }
            ]""";

    private static final String TOPIC_URL_PATTERN = "/.*sumSeries%28stats.tech.hermes\\."
            + "(consumer|producer)\\.%2A\\.meter\\.GROUP\\.TOPIC\\.m1_rate%29.*";

    private final WireMock graphiteListener;

    public PrometheusEndpoint(WireMockServer graphiteMock) {
        this.graphiteListener = new WireMock("localhost", graphiteMock.port());
    }

    public void returnMetricForTopic(String group, String topic, int rate, int deliveryRate) {
        String response = TOPIC_RESPONSE.replaceAll("TOPIC", group + "." + topic)
                .replaceAll("RATE", Integer.toString(rate))
                .replaceAll("DELIVERY", Integer.toString(deliveryRate))
                .replaceAll("TIMESTAMP", TIMESTAMP);
        String urlPattern = TOPIC_URL_PATTERN.replace("GROUP", group).replace("TOPIC", topic);
        graphiteListener.register(get(urlMatching(urlPattern))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    public void returnServerErrorForAllTopics() {
        graphiteListener.register(get(urlMatching(TOPIC_URL_PATTERN))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                )
        );
    }

    public void returnMetric(SubscriptionMetricsStubDefinition metricsStubDefinition) {
        graphiteListener.register(get(urlMatching(metricsStubDefinition.toUrlPattern()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(metricsStubDefinition.toBody())));
    }

    public void returnMetricWithDelay(SubscriptionMetricsStubDefinition metricsStubDefinition, int responseDelayInMs) {
        graphiteListener.register(get(urlMatching(metricsStubDefinition.toUrlPattern()))
                .willReturn(aResponse()
                        .withFixedDelay(responseDelayInMs)
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(metricsStubDefinition.toBody())));
    }



    private static class SubscriptionMetricsStubDefinition {
        private final String subscription;
        private final List<GraphiteStubResponse> responseBody;

        private SubscriptionMetricsStubDefinition(String subscription, List<GraphiteStubResponse> responseBody) {
            this.subscription = subscription;
            this.responseBody = responseBody;
        }

        private String toUrlPattern() {
            return "/.*sumSeries%28stats.tech.hermes\\.consumer\\.%2A\\.meter\\." + subscription + "\\.m1_rate%29.*";
        }

        private String toBody() {
            try {
                return new ObjectMapper().writeValueAsString(responseBody);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class SubscriptionMetricsStubDefinitionBuilder {
        private final String subscription;
        private final List<GraphiteStubResponse> response = new ArrayList<>();

        private SubscriptionMetricsStubDefinitionBuilder(String subscription) {
            this.subscription = subscription;
        }

        public SubscriptionMetricsStubDefinitionBuilder withRate(int rate) {
            String target = "sumSeries(stats.tech.hermes.consumer.*.meter." + subscription + ".m1_rate)";
            response.add(new GraphiteStubResponse(target, dataPointOf(rate)));
            return this;
        }

        public SubscriptionMetricsStubDefinitionBuilder withThroughput(int rate) {
            String target = "sumSeries(stats.tech.hermes.consumer.*.throughput." + subscription + ".m1_rate)";
            response.add(new GraphiteStubResponse(target, dataPointOf(rate)));
            return this;
        }

        public SubscriptionMetricsStubDefinitionBuilder withStatusRate(int httpStatus, int rate) {
            String statusFamily = httpStatusFamily(httpStatus);
            String target = "sumSeries(stats.tech.hermes.consumer.*.status." + subscription + "." + statusFamily + ".m1_rate)";
            response.add(new GraphiteStubResponse(target, dataPointOf(rate)));
            return this;
        }

        public SubscriptionMetricsStubDefinition build() {
            return new SubscriptionMetricsStubDefinition(subscription, response);
        }

        private String httpStatusFamily(int statusCode) {
            return format("%dxx", statusCode / 100);
        }

        private static List<List<Object>> dataPointOf(int rate) {
            return singletonList(asList(rate, TIMESTAMP));
        }
    }

    public static SubscriptionMetricsStubDefinitionBuilder subscriptionMetricsStub(String subscription) {
        return new SubscriptionMetricsStubDefinitionBuilder(subscription);
    }
}
