package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import com.google.common.base.Strings;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.api.MetricDecimalValue;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;

public class RestTemplatePrometheusClient implements PrometheusClient {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplatePrometheusClient.class);

    private static final ParameterizedTypeReference<List<PrometheusResponse>> PROMETHEUS_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private static final String DEFAULT_VALUE = "0.0";

    private static final String QUERY_PARAM = "query";

    private final URI prometheusUri;

    private final RestTemplate restTemplate;

    public RestTemplatePrometheusClient(RestTemplate restTemplate, URI prometheusUri) {
        this.restTemplate = restTemplate;
        this.prometheusUri = prometheusUri;
    }

    @Override
    public MonitoringMetricsContainer readMetrics(String... metricPaths) {
        try {
            MonitoringMetricsContainer response = new MonitoringMetricsContainer();
            queryPrometheus(metricPaths).forEach(metric -> response.addMetricValue(metric, ));
            return response;
        } catch (Exception exception) {
            logger.warn("Unable to read from Prometheus: {}", getRootCauseMessage(exception));
            return MonitoringMetricsContainer.unavailable(metricPaths);
        }
    }

    private boolean hasValidData(PrometheusResponse prometheusResponse) {
        return prometheusResponse.isSuccess();
    }

    private List<PrometheusResponse> queryPrometheus(String... queries) {
        UriBuilder builder = UriBuilder.fromUri(prometheusUri)
                .path("query");

        for (String query : queries) {
            builder.queryParam("query", query);
        }

        ResponseEntity<List<PrometheusResponse>> response = restTemplate.exchange(
                builder.build(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                PROMETHEUS_RESPONSE_TYPE
        );
        return response.getBody();
    }
}
