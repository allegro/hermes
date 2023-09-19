package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.api.MetricDecimalValue;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;


public class RestTemplatePrometheusClient implements PrometheusClient {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplatePrometheusClient.class);

    private final URI prometheusUri;
    private final RestTemplate restTemplate;

    public RestTemplatePrometheusClient(RestTemplate restTemplate, URI prometheusUri) {
        this.restTemplate = restTemplate;
        this.prometheusUri = prometheusUri;
    }

    @Override
    public MonitoringMetricsContainer readMetrics(String query) {
        try {
            PrometheusResponse response = queryPrometheus(query);
            Preconditions.checkNotNull(response, "Prometheus response is null");
            Preconditions.checkState(response.isSuccess(), "Prometheus response does not contain valid data");

            Map<String, List<PrometheusResponse.VectorResult>> metricsGroupedByName = groupMetricsByName(response);
            return produceMetricsContainer(metricsGroupedByName);
        } catch (Exception exception) {
            logger.warn("Unable to read from Prometheus...", exception);
            return MonitoringMetricsContainer.unavailable();
        }
    }

    private PrometheusResponse queryPrometheus(String query) {
        URI queryUri = URI.create(prometheusUri.toString() + "/api/v1/query?query=" + encode(query, UTF_8));

        ResponseEntity<PrometheusResponse> response = restTemplate.exchange(queryUri,
                HttpMethod.GET, HttpEntity.EMPTY, PrometheusResponse.class);
        return response.getBody();
    }

    private static Map<String, List<PrometheusResponse.VectorResult>> groupMetricsByName(PrometheusResponse response) {
        return response.data().results().stream()
                .map(RestTemplatePrometheusClient::renameStatusCodesMetricsNames)
                .collect(Collectors.groupingBy(r -> r.metricName().name()));
    }

    private static MonitoringMetricsContainer produceMetricsContainer(
            Map<String, List<PrometheusResponse.VectorResult>> metricsGroupedByName) {
        MonitoringMetricsContainer metricsContainer = MonitoringMetricsContainer.createEmpty();

        Stream<Pair<String, Double>> metricsSummedByStatusCodeFamily = metricsGroupedByName.entrySet().stream()
                .map(RestTemplatePrometheusClient::sumMetricsWithTheSameName);

        metricsSummedByStatusCodeFamily.forEach(pair -> metricsContainer.addMetricValue(
                pair.getKey(),
                MetricDecimalValue.of(pair.getValue().toString())));
        return metricsContainer;
    }

    private static PrometheusResponse.VectorResult renameStatusCodesMetricsNames(PrometheusResponse.VectorResult r) {
        /*
       Renames any metric containing status_code tag to the <metric_name>_2xx/3xx/4xx/5xx> metric name. For example:
       VectorResult(
           metricName=MetricName(
               name=hermes_consumers_subscription_http_status_codes_total,
               statusCode=Optional[200]),
           vector=[...]
        )
        ---->
        VectorResult(
           metricName=MetricName(
               name=hermes_consumers_subscription_http_status_codes_total_2xx,
               statusCode=Optional[200]),
           vector=[...]
        )
        It allows then to sum metrics accordingly to the status code family.
         */
        String suffix = "";
        if (r.metricName().is2xxStatusCode()) {
            suffix = "_2xx";
        } else if (r.metricName().is4xxStatusCode()) {
            suffix = "_4xx";
        } else if (r.metricName().is5xxStatusCode()) {
            suffix = "_5xx";
        }
        return r.renameMetric(r.metricName().name() + suffix);
    }

    /*
    We have to sum some metrics on the client side because Prometheus does not support this kind of aggregation when using
    query for multiple __name__ metrics.
     */
    private static Pair<String, Double> sumMetricsWithTheSameName(Map.Entry<String, List<PrometheusResponse.VectorResult>> e) {
        return Pair.of(
                e.getKey(),
                e.getValue().stream()
                        .map(PrometheusResponse.VectorResult::getValue)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .mapToDouble(d -> d).sum());
    }
}
