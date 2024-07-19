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
import pl.allegro.tech.hermes.management.infrastructure.metrics.MetricsQuery;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;


public class RestTemplateParallelPrometheusClient implements PrometheusClient {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateParallelPrometheusClient.class);

    private final URI prometheusUri;
    private final RestTemplate restTemplate;
    private final ExecutorService executorService;
    private final Duration parallelFetchingTimeoutMillis;

    public RestTemplateParallelPrometheusClient(RestTemplate restTemplate, URI prometheusUri,
                                                ExecutorService executorService, Duration parallelFetchingTimeoutMillis) {
        this.restTemplate = restTemplate;
        this.prometheusUri = prometheusUri;
        this.executorService = executorService;
        this.parallelFetchingTimeoutMillis = parallelFetchingTimeoutMillis;
    }

    @Override
    public MonitoringMetricsContainer readMetrics(List<MetricsQuery> queries) {
        MonitoringMetricsContainer metricsContainer = MonitoringMetricsContainer.createEmpty();
        fetchInParallelFromPrometheus(queries).forEach(metricsContainer::addMetricValue);
        return metricsContainer;
    }

    private MetricDecimalValue readSingleMetric(MetricsQuery query) {
        try {
            PrometheusResponse response = queryPrometheus(query);
            Preconditions.checkNotNull(response, "Prometheus response is null");
            Preconditions.checkState(response.isSuccess(), "Prometheus response does not contain valid data");

            return response.data().results().stream()
                    .findFirst()
                    .flatMap(PrometheusResponse.VectorResult::getValue)
                    .map(value -> MetricDecimalValue.of(value.toString()))
                    .orElse(MetricDecimalValue.defaultValue());
        } catch (Exception exception) {
            logger.warn("Unable to read from Prometheus...", exception);
            return MetricDecimalValue.unavailable();
        }
    }

    private Map<MetricsQuery, MetricDecimalValue> fetchInParallelFromPrometheus(List<MetricsQuery> queries) {
        // has to be collected (which is terminal operation) to run in parallel
        List<Pair<MetricsQuery, Future<MetricDecimalValue>>> pairs = queries.stream()
                .map(q -> Pair.of(q, executorService.submit(() -> readSingleMetric(q))))
                .toList();
        return pairs.stream().map(p -> {
            try {
                // don't need to set execution timeout as the http client has already configured proper timeouts
                return Pair.of(p.getKey(), p.getValue().get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Prometheus fetching thread was interrupted...", e);
                return Pair.of(p.getKey(), MetricDecimalValue.unavailable());
            } catch (ExecutionException ex) {
                logger.warn("Unexpected exception during fetching metrics from prometheus...", ex);
                return Pair.of(p.getKey(), MetricDecimalValue.unavailable());
            }
        }).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    private PrometheusResponse queryPrometheus(MetricsQuery query) {
        URI queryUri = URI.create(prometheusUri.toString() + "/api/v1/query?query=" + encode(query.query(), UTF_8));

        ResponseEntity<PrometheusResponse> response = restTemplate.exchange(queryUri,
                HttpMethod.GET, HttpEntity.EMPTY, PrometheusResponse.class);
        return response.getBody();
    }
}
