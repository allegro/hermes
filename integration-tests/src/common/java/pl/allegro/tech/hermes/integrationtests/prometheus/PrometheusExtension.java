package pl.allegro.tech.hermes.integrationtests.prometheus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusClient.forSubscription;
import static pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusClient.forSubscriptionStatusCode;
import static pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusClient.forTopic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;

public class PrometheusExtension
    implements AfterEachCallback, BeforeAllCallback, ExtensionContext.Store.CloseableResource {

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
    String deliveredQuery =
        forSubscription("hermes_consumers_subscription_delivered_total", subName, "");
    String timeoutsQuery =
        forSubscription("hermes_consumers_subscription_timeouts_total", subName, "");
    String retriesQuery =
        forSubscription("hermes_consumers_subscription_retries_total", subName, "");
    String throughputQuery =
        forSubscription("hermes_consumers_subscription_throughput_bytes_total", subName, "");
    String errorsQuery =
        forSubscription("hermes_consumers_subscription_other_errors_total", subName, "");
    String batchesQuery =
        forSubscription("hermes_consumers_subscription_batches_total", subName, "");
    String statusCodes2xxQuery =
        forSubscriptionStatusCode(
            "hermes_consumers_subscription_http_status_codes_total", subName, "2.*", "");
    String statusCodes4xxQuery =
        forSubscriptionStatusCode(
            "hermes_consumers_subscription_http_status_codes_total", subName, "4.*", "");
    String statusCodes5xxQuery =
        forSubscriptionStatusCode(
            "hermes_consumers_subscription_http_status_codes_total", subName, "5.*", "");

    stub(deliveredQuery, metrics.toPrometheusRateResponse());
    stub(timeoutsQuery, metrics.toPrometheusDefaultResponse());
    stub(retriesQuery, metrics.toPrometheusDefaultResponse());
    stub(throughputQuery, metrics.toPrometheusThroughputResponse());
    stub(errorsQuery, metrics.toPrometheusDefaultResponse());
    stub(batchesQuery, metrics.toPrometheusDefaultResponse());
    stub(statusCodes2xxQuery, metrics.toPrometheusStatusCodesResponse());
    stub(statusCodes4xxQuery, metrics.toPrometheusStatusCodesResponse());
    stub(statusCodes5xxQuery, metrics.toPrometheusStatusCodesResponse());
  }

  private void stub(String query, PrometheusResponse response) {
    wiremock.addStubMapping(
        get(urlPathEqualTo("/api/v1/query"))
            .withQueryParam("query", equalTo(query))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(writeValueAsString(response)))
            .build());
  }

  public void stubTopicMetrics(TopicMetrics metrics) {
    TopicName topicName = metrics.name();
    String requestsQuery = forTopic("hermes_frontend_topic_requests_total", topicName, "");
    String deliveredQuery =
        forTopic("hermes_consumers_subscription_delivered_total", topicName, "");
    String throughputQuery =
        forTopic("hermes_frontend_topic_throughput_bytes_total", topicName, "");

    stub(requestsQuery, metrics.toPrometheusRequestsResponse());
    stub(deliveredQuery, metrics.toDeliveredResponse());
    stub(throughputQuery, metrics.toPrometheusThroughputResponse());
  }

  public void stubDelay(Duration duration) {
    var response =
        new PrometheusResponse("success", new PrometheusResponse.Data("vector", List.of()));
    wiremock.addStubMapping(
        get(urlPathEqualTo("/api/v1/query"))
            .withQueryParam("query", new AnythingPattern())
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(writeValueAsString(response))
                    .withFixedDelay((int) duration.toMillis()))
            .build());
  }

  public void stub500Error() {
    wiremock.addStubMapping(
        get(urlPathEqualTo("/api/v1/query"))
            .withQueryParam("query", new AnythingPattern())
            .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json"))
            .build());
  }

  private String writeValueAsString(Object o) {
    try {
      return objectMapper.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
