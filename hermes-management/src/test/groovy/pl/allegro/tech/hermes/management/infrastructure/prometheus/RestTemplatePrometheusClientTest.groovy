package pl.allegro.tech.hermes.management.infrastructure.prometheus

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import jakarta.ws.rs.core.MediaType
import org.springframework.web.client.RestTemplate
import pl.allegro.tech.hermes.management.infrastructure.metrics.MetricsQuery
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Shared
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import static pl.allegro.tech.hermes.api.MetricDecimalValue.of

class RestTemplatePrometheusClientTest extends Specification {

    private static final int PROMETHEUS_HTTP_PORT = Ports.nextAvailable()
    def subscriptionDeliveredQuery = new MetricsQuery("sum by (group, topic, subscription) (irate({__name__=~'hermes_consumers_subscription_delivered_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1' service=~'hermes'}[1m]))")
    def subscriptionTimeoutsQuery = new MetricsQuery("sum by (group, topic, subscription) (irate({__name__=~'hermes_consumers_subscription_timeouts_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))")
    def subscriptionRetriesQuery = new MetricsQuery("sum by (group, topic, subscription) (irate({__name__=~'hermes_consumers_subscription_retries_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))")
    def subscriptionThroughputQuery = new MetricsQuery("sum by (group, topic, subscription) (irate({__name__=~'hermes_consumers_subscription_throughput_bytes_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))")
    def subscriptionErrorsQuery = new MetricsQuery("sum by (group, topic, subscription) (irate({__name__=~'hermes_consumers_subscription_other_errors_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))")
    def subscriptionBatchesQuery = new MetricsQuery("sum by (group, topic, subscription) (irate({__name__=~'hermes_consumers_subscription_batches_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))")
    def subscription2xxStatusCodesQuery = new MetricsQuery("sum by (group, topic, subscription) (irate({__name__=~'hermes_consumers_subscription_http_status_codes_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', status_code=~'2.*', service=~'hermes'}[1m]))")
    def subscription4xxStatusCodesQuery = new MetricsQuery("sum by (group, topic, subscription) (irate({__name__=~'hermes_consumers_subscription_http_status_codes_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', status_code=~'4.*', service=~'hermes'}[1m]))")
    def subscription5xxStatusCodesQuery = new MetricsQuery("sum by (group, topic, subscription) (irate({__name__=~'hermes_consumers_subscription_http_status_codes_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', status_code=~'5.*', service=~'hermes'}[1m]))")

    def queries = List.of(subscriptionDeliveredQuery, subscriptionTimeoutsQuery, subscriptionRetriesQuery, subscriptionThroughputQuery,
            subscriptionErrorsQuery, subscriptionBatchesQuery, subscription2xxStatusCodesQuery, subscription4xxStatusCodesQuery,
            subscription5xxStatusCodesQuery
    )

    @Shared
    WireMockServer wireMockServer = new WireMockServer(
            wireMockConfig()
                    .port(PROMETHEUS_HTTP_PORT).usingFilesUnderClasspath("prometheus-stubs"))

    private RestTemplateParallelPrometheusClient client

    void setupSpec() {
        wireMockServer.start()
    }

    void cleanupSpec() {
        wireMockServer.stop()
    }

    void setup() {
        wireMockServer.resetAll()
        ExecutorService executorService = Executors.newFixedThreadPool(10)
        RestTemplate restTemplate = new RestTemplate()
        client = new RestTemplateParallelPrometheusClient(restTemplate, URI.create("http://localhost:$PROMETHEUS_HTTP_PORT"), executorService, Duration.ofSeconds(5))
    }

    def "should get metrics for path"() {
        given:
        def queriesStubs = List.of(
                new QueryStub(subscriptionDeliveredQuery, "subscription_delivered_total.json"),
                new QueryStub(subscriptionTimeoutsQuery, "subscription_timeouts_total.json"),
                new QueryStub(subscriptionRetriesQuery, "subscription_retries_total.json"),
                new QueryStub(subscriptionThroughputQuery, "subscription_throughput_bytes_total.json"),
                new QueryStub(subscriptionErrorsQuery, "subscription_other_errors_total.json"),
                new QueryStub(subscriptionBatchesQuery, "subscription_batches_total.json"),
                new QueryStub(subscription2xxStatusCodesQuery, "subscription_2xx_http_status_codes_total.json"),
                new QueryStub(subscription4xxStatusCodesQuery, "subscription_4xx_http_status_codes_total.json"),
                new QueryStub(subscription5xxStatusCodesQuery, "subscription_5xx_http_status_codes_total.json"),
        )
        mockPrometheus(queriesStubs)

        when:
        MonitoringMetricsContainer metrics = client.readMetrics(queries)

        then:
        metrics.metricValue(subscriptionDeliveredQuery) == of("1.0")
        metrics.metricValue(subscriptionTimeoutsQuery) == of("2.0")
        metrics.metricValue(subscriptionRetriesQuery) == of("1.0")
        metrics.metricValue(subscriptionThroughputQuery) == of("3.0")
        metrics.metricValue(subscriptionErrorsQuery) == of("4.0")
        metrics.metricValue(subscriptionBatchesQuery) == of("5.0")
        metrics.metricValue(subscription2xxStatusCodesQuery) == of("2.0")
        metrics.metricValue(subscription4xxStatusCodesQuery) == of("1.0")
        metrics.metricValue(subscription5xxStatusCodesQuery) == of("2.0")
    }

    def "should return default value when metric has no value"() {
        given:
        def queriesStubs = List.of(
                emptyStub(subscriptionDeliveredQuery),
                new QueryStub(subscriptionTimeoutsQuery, "subscription_timeouts_total.json"),
                new QueryStub(subscriptionRetriesQuery, "subscription_retries_total.json"),
                emptyStub(subscriptionThroughputQuery),
                emptyStub(subscriptionErrorsQuery),
                emptyStub(subscriptionBatchesQuery),
                emptyStub(subscription2xxStatusCodesQuery),
                emptyStub(subscription4xxStatusCodesQuery),
                emptyStub(subscription5xxStatusCodesQuery)
        )
        mockPrometheus(queriesStubs)

        when:
        MonitoringMetricsContainer metrics = client.readMetrics(queries)

        then:
        metrics.metricValue(subscriptionDeliveredQuery) == of("0.0")
        metrics.metricValue(subscriptionTimeoutsQuery) == of("2.0")
        metrics.metricValue(subscriptionRetriesQuery) == of("1.0")
        metrics.metricValue(subscriptionThroughputQuery) == of("0.0")
        metrics.metricValue(subscriptionErrorsQuery) == of("0.0")
        metrics.metricValue(subscriptionBatchesQuery) == of("0.0")
        metrics.metricValue(subscription2xxStatusCodesQuery) == of("0.0")
        metrics.metricValue(subscription4xxStatusCodesQuery) == of("0.0")
        metrics.metricValue(subscription5xxStatusCodesQuery) == of("0.0")
    }

    private void mockPrometheus(List<QueryStub> queries) {
        queries.forEach { q ->
            String encodedQuery = URLEncoder.encode(q.query.query(), StandardCharsets.UTF_8)
            wireMockServer.stubFor(WireMock.get(urlEqualTo(String.format("/api/v1/query?query=%s", encodedQuery)))
                    .willReturn(WireMock.aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .withBodyFile(q.fileName)))
        }
    }

    static class QueryStub {
        QueryStub(MetricsQuery query, String fileName) {
            this.query = query
            this.fileName = fileName
        }
        MetricsQuery query;
        String fileName
    }

    QueryStub emptyStub(MetricsQuery query) {
        return new QueryStub(query, "prometheus_empty_response.json")
    }
}
