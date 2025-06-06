package pl.allegro.tech.hermes.management.infrastructure.prometheus


import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.ws.rs.core.MediaType
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.core5.util.Timeout
import org.junit.Rule
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import static pl.allegro.tech.hermes.api.MetricDecimalValue.defaultValue
import static pl.allegro.tech.hermes.api.MetricDecimalValue.of
import static pl.allegro.tech.hermes.api.MetricDecimalValue.unavailable

class RestTemplatePrometheusClientTest extends Specification {

    private static final int PROMETHEUS_HTTP_PORT = Ports.nextAvailable()
    def subscriptionDeliveredQuery = "sum by (group, topic, subscription) (irate({__name__='hermes_consumers_subscription_delivered_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1' service=~'hermes'}[1m]))"
    def subscriptionTimeoutsQuery = "sum by (group, topic, subscription) (irate({__name__='hermes_consumers_subscription_timeouts_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))"
    def subscriptionRetriesQuery = "sum by (group, topic, subscription) (irate({__name__='hermes_consumers_subscription_retries_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))"
    def subscriptionThroughputQuery = "sum by (group, topic, subscription) (irate({__name__='hermes_consumers_subscription_throughput_bytes_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))"
    def subscriptionErrorsQuery = "sum by (group, topic, subscription) (irate({__name__='hermes_consumers_subscription_other_errors_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))"
    def subscriptionBatchesQuery = "sum by (group, topic, subscription) (irate({__name__='hermes_consumers_subscription_batches_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', service=~'hermes'}[1m]))"
    def subscription2xxStatusCodesQuery = "sum by (group, topic, subscription) (irate({__name__='hermes_consumers_subscription_http_status_codes_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', status_code=~'2.*', service=~'hermes'}[1m]))"
    def subscription4xxStatusCodesQuery = "sum by (group, topic, subscription) (irate({__name__='hermes_consumers_subscription_http_status_codes_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', status_code=~'4.*', service=~'hermes'}[1m]))"
    def subscription5xxStatusCodesQuery = "sum by (group, topic, subscription) (irate({__name__='hermes_consumers_subscription_http_status_codes_total', group='pl.allegro.tech.hermes', topic='Monitor', subscription='consumer1', status_code=~'5.*', service=~'hermes'}[1m]))"

    def queries = List.of(subscriptionDeliveredQuery, subscriptionTimeoutsQuery, subscriptionRetriesQuery, subscriptionThroughputQuery,
            subscriptionErrorsQuery, subscriptionBatchesQuery, subscription2xxStatusCodesQuery, subscription4xxStatusCodesQuery,
            subscription5xxStatusCodesQuery
    )

    @Rule
    WireMockRule wireMockServer = new WireMockRule(
            wireMockConfig()
                    .port(PROMETHEUS_HTTP_PORT).usingFilesUnderClasspath("prometheus-stubs"))

    private RestTemplatePrometheusClient client

    void setup() {
        ExecutorService executorService = Executors.newFixedThreadPool(10)
        RestTemplate restTemplate = createRestTemplateWithTimeout(Duration.ofSeconds(1))
        client = new RestTemplatePrometheusClient(restTemplate, URI.create("http://localhost:$PROMETHEUS_HTTP_PORT"),
                executorService, Duration.ofSeconds(5), new SimpleMeterRegistry())
        wireMockServer.resetAll()
    }

    def "should get metrics for path"() {
        given:
        def queriesStubs = List.of(
                new FileStub(subscriptionDeliveredQuery, "subscription_delivered_total.json"),
                new FileStub(subscriptionTimeoutsQuery, "subscription_timeouts_total.json"),
                new FileStub(subscriptionRetriesQuery, "subscription_retries_total.json"),
                new FileStub(subscriptionThroughputQuery, "subscription_throughput_bytes_total.json"),
                new FileStub(subscriptionErrorsQuery, "subscription_other_errors_total.json"),
                new FileStub(subscriptionBatchesQuery, "subscription_batches_total.json"),
                new FileStub(subscription2xxStatusCodesQuery, "subscription_2xx_http_status_codes_total.json"),
                new FileStub(subscription4xxStatusCodesQuery, "subscription_4xx_http_status_codes_total.json"),
                new FileStub(subscription5xxStatusCodesQuery, "subscription_5xx_http_status_codes_total.json"),
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
                new FileStub(subscriptionTimeoutsQuery, "subscription_timeouts_total.json"),
                new FileStub(subscriptionRetriesQuery, "subscription_retries_total.json"),
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
        metrics.metricValue(subscriptionDeliveredQuery) == defaultValue()
        metrics.metricValue(subscriptionTimeoutsQuery) == of("2.0")
        metrics.metricValue(subscriptionRetriesQuery) == of("1.0")
        metrics.metricValue(subscriptionThroughputQuery) == defaultValue()
        metrics.metricValue(subscriptionErrorsQuery) == defaultValue()
        metrics.metricValue(subscriptionBatchesQuery) == defaultValue()
        metrics.metricValue(subscription2xxStatusCodesQuery) == defaultValue()
        metrics.metricValue(subscription4xxStatusCodesQuery) == defaultValue()
        metrics.metricValue(subscription5xxStatusCodesQuery) == defaultValue()
    }

    def "should return partial results when some of the requests fails"() {
        given:
        def queriesToFail = List.of(
                subscriptionDeliveredQuery,
                subscriptionThroughputQuery,
                subscriptionErrorsQuery,
                subscriptionBatchesQuery,
                subscription2xxStatusCodesQuery,
                subscription4xxStatusCodesQuery,
                subscription5xxStatusCodesQuery,
        )
        def queriesToSuccess = List.of(
                new FileStub(subscriptionTimeoutsQuery, "subscription_timeouts_total.json"),
                new FileStub(subscriptionRetriesQuery, "subscription_retries_total.json"),
        )
        mockPrometheus(queriesToSuccess)
        mockPrometheusTimeout(queriesToFail, Duration.ofSeconds(5))

        when:
        MonitoringMetricsContainer metrics = client.readMetrics(queries)

        then:
        metrics.metricValue(subscriptionDeliveredQuery) == unavailable()
        metrics.metricValue(subscriptionTimeoutsQuery) == of("2.0")
        metrics.metricValue(subscriptionRetriesQuery) == of("1.0")
        metrics.metricValue(subscriptionThroughputQuery) == unavailable()
        metrics.metricValue(subscriptionErrorsQuery) == unavailable()
        metrics.metricValue(subscriptionBatchesQuery) == unavailable()
        metrics.metricValue(subscription2xxStatusCodesQuery) == unavailable()
        metrics.metricValue(subscription4xxStatusCodesQuery) == unavailable()
        metrics.metricValue(subscription5xxStatusCodesQuery) == unavailable()
    }

    private void mockPrometheus(List<FileStub> stubs) {
        stubs.forEach { s ->
            String encodedQuery = URLEncoder.encode(s.query, StandardCharsets.UTF_8)
            wireMockServer.stubFor(WireMock.get(urlEqualTo(String.format("/api/v1/query?query=%s", encodedQuery)))
                    .willReturn(WireMock.aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .withBodyFile(s.fileName)))
        }
    }

    private void mockPrometheusTimeout(List<String> queries, Duration delay) {
        queries.forEach { q ->
            String encodedQuery = URLEncoder.encode(q, StandardCharsets.UTF_8)
            wireMockServer.stubFor(WireMock.get(urlEqualTo(String.format("/api/v1/query?query=%s", encodedQuery)))
                    .willReturn(WireMock.aResponse()
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .withFixedDelay(delay.toMillis() as Integer)));
        }
    }

    private RestTemplate createRestTemplateWithTimeout(Duration timeout) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(timeout.toMillis()))
                .setResponseTimeout(Timeout.ofMilliseconds(timeout.toMillis()))
                .build();

        HttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();

        ClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(client);
        return new RestTemplate(clientHttpRequestFactory);
    }

    static class FileStub {
        FileStub(String query, String fileName) {
            this.query = query
            this.fileName = fileName
        }
        String query;
        String fileName
    }

    FileStub emptyStub(String query) {
        return new FileStub(query, "prometheus_empty_response.json")
    }
}
