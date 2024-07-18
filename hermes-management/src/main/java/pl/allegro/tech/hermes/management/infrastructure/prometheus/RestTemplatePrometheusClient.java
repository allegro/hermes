package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import com.google.common.base.Preconditions;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;


public class RestTemplatePrometheusClient implements PrometheusClient {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplatePrometheusClient.class);

    private final URI prometheusUri;
    private final RestTemplate restTemplate;
    private final ExecutorService executorService;

    public RestTemplatePrometheusClient(RestTemplate restTemplate, URI prometheusUri,
                                        ExecutorService executorService) {
        this.restTemplate = restTemplate;
        this.prometheusUri = prometheusUri;
        this.executorService = executorService;
    }

    @Override
    public MonitoringMetricsContainer readMetrics(List<Query> queries) {
        MonitoringMetricsContainer metricsContainer = MonitoringMetricsContainer.createEmpty();
        fetchInParallelFromPrometheus(queries).forEach(
                (Map<String, MetricDecimalValue> resultsFromSingleQuery) -> resultsFromSingleQuery.forEach(
                        metricsContainer::addMetricValue));
        return metricsContainer;
    }

    private Map<String, MetricDecimalValue> readSingleMetric(Query query) {
        try {
            PrometheusResponse response = queryPrometheus(query);
            Preconditions.checkNotNull(response, "Prometheus response is null");
            Preconditions.checkState(response.isSuccess(), "Prometheus response does not contain valid data");

            return groupMetricsByNameCombinedWithStatusCodeFamily(query.name(), response)
                    .entrySet().stream()
                    .map(RestTemplatePrometheusClient::sumMetricsWithTheSameName)
                    .map(p -> Pair.of(p.getKey(), MetricDecimalValue.of(p.getValue().toString())))
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        } catch (Exception exception) {
            logger.warn("Unable to read from Prometheus...", exception);
            return Map.of();
        }
    }

    private List<Map<String, MetricDecimalValue>> fetchInParallelFromPrometheus(List<Query> fullQueries) {
        // has to be collected (which is terminal operation) to run in parallel
        List<Future<Map<String, MetricDecimalValue>>> futures = fullQueries.stream()
                .map(q -> executorService.submit(() -> readSingleMetric(q)))
                .toList();
        return futures.stream().map(f -> {
            try {
                // don't need to set execution timeout as the http client has already configured proper timeouts
                return f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Prometheus fetching thread was interrupted...", e);
                return Map.<String, MetricDecimalValue>of();
            } catch (ExecutionException ex) {
                logger.warn("Unexpected exception during fetching metrics from prometheus...", ex);
                return Map.<String, MetricDecimalValue>of();
            }
        }).toList();
    }

    private PrometheusResponse queryPrometheus(Query query) {
        URI queryUri = URI.create(prometheusUri.toString() + "/api/v1/query?query=" + encode(query.fullQuery(), UTF_8));

        ResponseEntity<PrometheusResponse> response = restTemplate.exchange(queryUri,
                HttpMethod.GET, HttpEntity.EMPTY, PrometheusResponse.class);
        return response.getBody();
    }

    private static Map<String, List<PrometheusResponse.VectorResult>> groupMetricsByNameCombinedWithStatusCodeFamily(
            String queryName, PrometheusResponse response) {
        return response.data().results().stream()
                .collect(Collectors.groupingBy(r -> queryName + r.metricName().statusCodeFamilySuffix()));
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
