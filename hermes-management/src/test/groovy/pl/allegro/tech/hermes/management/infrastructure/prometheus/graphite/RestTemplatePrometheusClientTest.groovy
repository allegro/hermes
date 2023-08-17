package pl.allegro.tech.hermes.management.infrastructure.prometheus.graphite

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import jakarta.ws.rs.core.MediaType
import org.junit.Rule
import org.springframework.web.client.RestTemplate
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer
import pl.allegro.tech.hermes.management.infrastructure.prometheus.RestTemplatePrometheusClient
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Specification

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import static java.nio.charset.Charset.defaultCharset
import static pl.allegro.tech.hermes.api.MetricDecimalValue.of

class RestTemplatePrometheusClientTest extends Specification {

    private static final int PROMETHEUS_HTTP_PORT = Ports.nextAvailable()
    private static final String query = "sum({__name__=~'hermes_consumers_subscription_delivered_total" +
            "|hermes_consumers_subscription_timeouts_total" +
            "|hermes_consumers_subscription_throughput_bytes_total" +
            "|hermes_consumers_subscription_other_errors_total" +
            "|hermes_consumers_subscription_batches_total" +
            "|hermes_consumers_subscription_http_status_codes_total'," +
            "group='pl.allegro.tech.hermes',topic='hermesTopic',subscription='hermesSubscription'})" +
            "by(__name__,group,topic,subscription,status_code)"


    @Rule
    WireMockRule wireMockRule = new WireMockRule(
            wireMockConfig().port(PROMETHEUS_HTTP_PORT).usingFilesUnderClasspath("prometheus-stubs"))

    private RestTemplatePrometheusClient client

    void setup() {
        RestTemplate restTemplate = new RestTemplate();
        client = new RestTemplatePrometheusClient(restTemplate, URI.create("http://localhost:$PROMETHEUS_HTTP_PORT"),);
    }

    def "should get metrics for path"() {
        given:
        mockPrometheus(query, "full_response.json");

        when:
        MonitoringMetricsContainer metrics = client.readMetrics(query)

        then:
        metrics.metricValue("hermes_consumers_subscription_delivered_total") == of("1.0")
        metrics.metricValue("hermes_consumers_subscription_timeouts_total") == of("2.0")
        metrics.metricValue("hermes_consumers_subscription_throughput_bytes_total") == of("3.0")
        metrics.metricValue("hermes_consumers_subscription_other_errors_total") == of("4.0")
        metrics.metricValue("hermes_consumers_subscription_batches_total") == of("5.0")
        metrics.metricValue("hermes_consumers_subscription_http_status_codes_total_2xx") == of("2.0")
        metrics.metricValue("hermes_consumers_subscription_http_status_codes_total_4xx") == of("1.0")
        metrics.metricValue("hermes_consumers_subscription_http_status_codes_total_5xx") == of("2.0")
    }

    def "should return default value when metric has no value"() {
        given:
        mockPrometheus(query, "partial_response.json")

        when:
        MonitoringMetricsContainer metrics = client.readMetrics(query)

        then:
        metrics.metricValue("hermes_consumers_subscription_delivered_total") == of("0.0")
        metrics.metricValue("hermes_consumers_subscription_timeouts_total") == of("2.0")
        metrics.metricValue("hermes_consumers_subscription_throughput_bytes_total") == of("3.0")
        metrics.metricValue("hermes_consumers_subscription_other_errors_total") == of("4.0")
        metrics.metricValue("hermes_consumers_subscription_batches_total") == of("5.0")
        metrics.metricValue("hermes_consumers_subscription_http_status_codes_total_2xx") == of("2.0")
        metrics.metricValue("hermes_consumers_subscription_http_status_codes_total_4xx") == of("1.0")
        metrics.metricValue("hermes_consumers_subscription_http_status_codes_total_5xx") == of("0.0")
    }

    private void mockPrometheus(String query, String responseFile) {
        String encodedQuery = URLEncoder.encode(query, defaultCharset())
        WireMock.stubFor(WireMock.get(urlEqualTo(String.format("/api/v1/query?query=%s", encodedQuery)))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBodyFile(responseFile)))
    }
}
