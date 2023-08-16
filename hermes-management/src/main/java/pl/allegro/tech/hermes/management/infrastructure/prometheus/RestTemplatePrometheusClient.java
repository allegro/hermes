package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import jakarta.ws.rs.core.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;

import java.net.URI;

import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;


public class RestTemplatePrometheusClient implements PrometheusClient {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplatePrometheusClient.class);

    private static final String DEFAULT_VALUE = "0.0";

    private static final String QUERY_PARAM = "query";

    private final URI prometheusUri;

    private final RestTemplate restTemplate;

    public RestTemplatePrometheusClient(RestTemplate restTemplate, URI prometheusUri) {
        this.restTemplate = restTemplate;
        this.prometheusUri = prometheusUri;
    }

    @Override
    public MonitoringMetricsContainer readMetrics(String query) {
        try {
            MonitoringMetricsContainer response = new MonitoringMetricsContainer();
            PrometheusResponse response = queryPrometheus(query);
            return response;
        } catch (Exception exception) {
            logger.warn("Unable to read from Prometheus: {}", getRootCauseMessage(exception));
            return MonitoringMetricsContainer.unavailable(query);
        }
    }

    private boolean hasValidData(PrometheusResponse prometheusResponse) {
        return prometheusResponse.isSuccess();
    }

    private PrometheusResponse queryPrometheus(String query) {
        UriBuilder builder = UriBuilder.fromUri(prometheusUri)
                .path("api/v1/query");

        builder.queryParam("query", query);

        ResponseEntity<PrometheusResponse> response = restTemplate.exchange(
                builder.build(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                PrometheusResponse.class
        );
        return response.getBody();
    }
}
