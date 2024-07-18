package pl.allegro.tech.hermes.management.infrastructure.prometheus

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import jakarta.ws.rs.core.MediaType
import org.springframework.web.client.RestTemplate
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Shared
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import static pl.allegro.tech.hermes.api.MetricDecimalValue.of

class RestTemplatePrometheusClientTest extends Specification {

    private static final int PROMETHEUS_HTTP_PORT = Ports.nextAvailable()
    def subscriptionDeliveredQuery = new PrometheusClient.Query("hermes_consumers_subscription_delivered_total", "sum by (group, topic, subscription, status_code) (irate({__name__=~'hermes_consumers_subscription_delivered_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))")
    def subscriptionTimeoutsQuery = new PrometheusClient.Query("hermes_consumers_subscription_timeouts_total", "sum by (group, topic, subscription, status_code) (irate({__name__=~'hermes_consumers_subscription_timeouts_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))")
    def subscriptionRetriesQuery = new PrometheusClient.Query("hermes_consumers_subscription_retries_total", "sum by (group, topic, subscription, status_code) (irate({__name__=~'hermes_consumers_subscription_retries_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))")
    def subscriptionThroughputQuery = new PrometheusClient.Query("hermes_consumers_subscription_throughput_bytes_total", "sum by (group, topic, subscription, status_code) (irate({__name__=~'hermes_consumers_subscription_throughput_bytes_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))")
    def subscriptionErrorsQuery = new PrometheusClient.Query("hermes_consumers_subscription_other_errors_total", "sum by (group, topic, subscription, status_code) (irate({__name__=~'hermes_consumers_subscription_other_errors_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))")
    def subscriptionBatchesQuery = new PrometheusClient.Query("hermes_consumers_subscription_batches_total", "sum by (group, topic, subscription, status_code) (irate({__name__=~'hermes_consumers_subscription_batches_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))")
    def subscriptionStatusCodesQuery = new PrometheusClient.Query("hermes_consumers_subscription_http_status_codes_total", "sum by (group, topic, subscription, status_code) (irate({__name__=~'hermes_consumers_subscription_http_status_codes_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))")
    def queries = List.of(subscriptionDeliveredQuery, subscriptionTimeoutsQuery, subscriptionRetriesQuery, subscriptionThroughputQuery,
            subscriptionErrorsQuery, subscriptionBatchesQuery, subscriptionStatusCodesQuery)

    @Shared
    WireMockServer wireMockServer = new WireMockServer(
            wireMockConfig().port(PROMETHEUS_HTTP_PORT).usingFilesUnderClasspath("prometheus-stubs"))

    private RestTemplatePrometheusClient client

    void setupSpec() {
        wireMockServer.start()
    }

    void cleanupSpec() {
        wireMockServer.stop()
    }

    void setup() {
        wireMockServer.resetAll()
        ExecutorService executorService = Executors.newFixedThreadPool(7)
        RestTemplate restTemplate = new RestTemplate()
        client = new RestTemplatePrometheusClient(restTemplate, URI.create("http://localhost:$PROMETHEUS_HTTP_PORT"), executorService)
    }

    def "should get metrics for path"() {
        given:
        mockPrometheus(queries)

        when:
        MonitoringMetricsContainer metrics = client.readMetrics(queries)

        then:
        metrics.metricValue("hermes_consumers_subscription_delivered_total") == of("1.0")
        metrics.metricValue("hermes_consumers_subscription_timeouts_total") == of("2.0")
        metrics.metricValue("hermes_consumers_subscription_retries_total") == of("1.0")
        metrics.metricValue("hermes_consumers_subscription_throughput_bytes_total") == of("3.0")
        metrics.metricValue("hermes_consumers_subscription_other_errors_total") == of("4.0")
        metrics.metricValue("hermes_consumers_subscription_batches_total") == of("5.0")
        metrics.metricValue("hermes_consumers_subscription_http_status_codes_total_2xx") == of("2.0")
        metrics.metricValue("hermes_consumers_subscription_http_status_codes_total_4xx") == of("1.0")
        metrics.metricValue("hermes_consumers_subscription_http_status_codes_total_5xx") == of("2.0")
    }

    def "should return default value when metric has no value"() {
        given:
        mockPrometheus(List.of(subscriptionTimeoutsQuery, subscriptionRetriesQuery))

        when:
        MonitoringMetricsContainer metrics = client.readMetrics(queries)

        then:
        metrics.metricValue("hermes_consumers_subscription_delivered_total") == of("0.0")
        metrics.metricValue("hermes_consumers_subscription_timeouts_total") == of("2.0")
        metrics.metricValue("hermes_consumers_subscription_retries_total") == of("1.0")
        metrics.metricValue("hermes_consumers_subscription_throughput_bytes_total") == of("0.0")
        metrics.metricValue("hermes_consumers_subscription_other_errors_total") == of("0.0")
        metrics.metricValue("hermes_consumers_subscription_batches_total") == of("0.0")
        metrics.metricValue("hermes_consumers_subscription_http_status_codes_total_2xx") == of("0.0")
        metrics.metricValue("hermes_consumers_subscription_http_status_codes_total_4xx") == of("0.0")
        metrics.metricValue("hermes_consumers_subscription_http_status_codes_total_5xx") == of("0.0")
    }

    private void mockPrometheus(List<PrometheusClient.Query> queries) {
        queries.forEach { q ->
            String responseFile = q.name().replace("hermes_consumers_", "") + ".json"
            String encodedQuery = URLEncoder.encode(q.fullQuery(), StandardCharsets.UTF_8)
            wireMockServer.stubFor(WireMock.get(urlEqualTo(String.format("/api/v1/query?query=%s", encodedQuery)))
                    .willReturn(WireMock.aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .withBodyFile(responseFile)))
        }

    }
}
