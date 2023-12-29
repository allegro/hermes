package pl.allegro.tech.hermes.integrationtests.prometheus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class PrometheusExtension implements AfterEachCallback, BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static final WireMockServer wiremock = new WireMockServer(0);
    private static boolean started = false;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!started) {
            wiremock.start();
            started = true;
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        wiremock.resetAll();
    }

    @Override
    public void close() {
        wiremock.shutdown();
    }

    public String getEndpoint() {
        return "http://localhost:" + wiremock.port();
    }

    public void stubSubscriptionMetrics(SubscriptionMetrics metrics) {
        SubscriptionName name = metrics.name();
        String query = """
                sum by (__name__, group, topic, subscription, status_code) (
                  irate(
                    {__name__=~'hermes_consumers_subscription_delivered_total|hermes_consumers_subscription_timeouts_total|hermes_consumers_subscription_throughput_bytes_total|hermes_consumers_subscription_other_errors_total|hermes_consumers_subscription_batches_total|hermes_consumers_subscription_http_status_codes_total', group='%s', topic='%s', subscription='%s', }[1m]
                  ) keep_metric_names
                )
                """
                .formatted(name.getTopicName().getGroupName(), name.getTopicName().getName(), name.getName())
                .lines()
                .map(String::stripLeading)
                .collect(Collectors.joining());
        wiremock.addStubMapping(
                get(urlPathEqualTo("/api/v1/query"))
                        .withQueryParam("query", equalTo(query))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(writeValueAsString(metrics.toPrometheusResponse()))
                        )
                        .build()
        );
    }

    public void stubTopicMetrics(TopicMetrics metrics) {
        TopicName name = metrics.name();
        String query = """
                sum by (__name__, group, topic) (
                  irate(
                    {__name__=~'hermes_frontend_topic_requests_total|hermes_consumers_subscription_delivered_total|hermes_frontend_topic_throughput_bytes_total', group='%s', topic='%s', }[1m]
                  ) keep_metric_names
                )
                """
                .formatted(name.getGroupName(), name.getName())
                .lines()
                .map(String::stripLeading)
                .collect(Collectors.joining());
        wiremock.addStubMapping(
                get(urlPathEqualTo("/api/v1/query"))
                        .withQueryParam("query", equalTo(query))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(writeValueAsString(metrics.toPrometheusResponse()))
                        )
                        .build()
        );
    }

    public void stubDelay(Duration duration) {
        var response = new PrometheusResponse("success", new PrometheusResponse.Data("vector", List.of()));
        wiremock.addStubMapping(
                get(urlPathEqualTo("/api/v1/query"))
                        .withQueryParam("query", new AnythingPattern())
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(writeValueAsString(response))
                                        .withFixedDelay((int) duration.toMillis())
                        )
                        .build()
        );
    }

    public void stub500Error() {
        wiremock.addStubMapping(
                get(urlPathEqualTo("/api/v1/query"))
                        .withQueryParam("query", new AnythingPattern())
                        .willReturn(
                                aResponse()
                                        .withStatus(500)
                                        .withHeader("Content-Type", "application/json")
                        )
                        .build()
        );
    }

    private String writeValueAsString(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
