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
import pl.allegro.tech.hermes.management.infrastructure.metrics.MetricsQuery;

import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusClient.forSubscription;
import static pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusClient.forSubscriptionStatusCode;
import static pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusClient.forTopic;

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

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void stubSubscriptionMetrics(SubscriptionMetrics metrics) {
        SubscriptionName subName = metrics.name();
        MetricsQuery deliveredQuery = forSubscription("hermes_consumers_subscription_delivered_total", subName, "");
        MetricsQuery timeoutsQuery = forSubscription("hermes_consumers_subscription_timeouts_total", subName, "");
        MetricsQuery retriesQuery = forSubscription("hermes_consumers_subscription_retries_total", subName, "");
        MetricsQuery throughputQuery = forSubscription("hermes_consumers_subscription_throughput_bytes_total", subName, "");
        MetricsQuery errorsQuery = forSubscription("hermes_consumers_subscription_other_errors_total", subName, "");
        MetricsQuery batchesQuery = forSubscription("hermes_consumers_subscription_batches_total", subName, "");
        MetricsQuery statusCodes2xxQuery = forSubscriptionStatusCode("hermes_consumers_subscription_http_status_codes_total", subName, "2.*", "");
        MetricsQuery statusCodes4xxQuery = forSubscriptionStatusCode("hermes_consumers_subscription_http_status_codes_total", subName, "4.*", "");
        MetricsQuery statusCodes5xxQuery = forSubscriptionStatusCode("hermes_consumers_subscription_http_status_codes_total", subName, "5.*", "");

        stub(deliveredQuery.query(), metrics.toPrometheusRateResponse());
        stub(timeoutsQuery.query(), metrics.toPrometheusDefaultResponse());
        stub(retriesQuery.query(), metrics.toPrometheusDefaultResponse());
        stub(throughputQuery.query(), metrics.toPrometheusThroughputResponse());
        stub(errorsQuery.query(), metrics.toPrometheusDefaultResponse());
        stub(batchesQuery.query(), metrics.toPrometheusDefaultResponse());
        stub(statusCodes2xxQuery.query(), metrics.toPrometheusStatusCodesResponse());
        stub(statusCodes4xxQuery.query(), metrics.toPrometheusStatusCodesResponse());
        stub(statusCodes5xxQuery.query(), metrics.toPrometheusStatusCodesResponse());
    }

    private void stub(String query, PrometheusResponse response) {
        wiremock.addStubMapping(
                get(urlPathEqualTo("/api/v1/query"))
                        .withQueryParam("query", equalTo(query))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(writeValueAsString(response))
                        )
                        .build()
        );
    }

    public void stubTopicMetrics(TopicMetrics metrics) {
        TopicName topicName = metrics.name();
        MetricsQuery requestsQuery = forTopic("hermes_frontend_topic_requests_total", topicName, "");
        MetricsQuery deliveredQuery = forTopic("hermes_consumers_subscription_delivered_total", topicName, "");
        MetricsQuery throughputQuery = forTopic("hermes_frontend_topic_throughput_bytes_total", topicName, "");

        stub(requestsQuery.query(), metrics.toPrometheusRequestsResponse());
        stub(deliveredQuery.query(), metrics.toDeliveredResponse());
        stub(throughputQuery.query(), metrics.toPrometheusThroughputResponse());
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
