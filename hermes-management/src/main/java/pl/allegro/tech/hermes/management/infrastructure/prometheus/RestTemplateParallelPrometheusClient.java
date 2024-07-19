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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;


public class RestTemplateParallelPrometheusClient implements PrometheusClient {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateParallelPrometheusClient.class);

    private final URI prometheusUri;
    private final RestTemplate restTemplate;
    private final ExecutorService executorService;
    private final Duration parallelFetchingTimeout;

    public RestTemplateParallelPrometheusClient(RestTemplate restTemplate, URI prometheusUri,
                                                ExecutorService executorService, Duration parallelFetchingTimeoutMillis) {
        this.restTemplate = restTemplate;
        this.prometheusUri = prometheusUri;
        this.executorService = executorService;
        this.parallelFetchingTimeout = parallelFetchingTimeoutMillis;
    }

    @Override
    public MonitoringMetricsContainer readMetrics(List<MetricsQuery> queries) {
        return fetchInParallelFromPrometheus(queries);
    }

    private MonitoringMetricsContainer fetchInParallelFromPrometheus(List<MetricsQuery> queries) {
        // has to be collected (which is terminal operation) to run in parallel
        CompletableFuture<Map<MetricsQuery, MetricDecimalValue>> aggregatedFuture = getAggregatedCompletableFuture(queries);

        try {
            Map<MetricsQuery, MetricDecimalValue> metrics = aggregatedFuture.get(parallelFetchingTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return MonitoringMetricsContainer.initialized(metrics);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Prometheus fetching thread was interrupted...", e);
            return MonitoringMetricsContainer.unavailable();
        } catch (Exception ex) {
            logger.warn("Unexpected exception during fetching metrics from prometheus...", ex);
            return MonitoringMetricsContainer.unavailable();
        }
    }

    private CompletableFuture<Pair<MetricsQuery, MetricDecimalValue>> readSingleMetric(MetricsQuery query) {
        try {
            CompletableFuture<PrometheusResponse> future = CompletableFuture.supplyAsync(
                    () -> queryPrometheus(query), executorService);
            return future.thenApply(response -> {
                Preconditions.checkNotNull(response, "Prometheus response is null");
                Preconditions.checkState(response.isSuccess(), "Prometheus response does not contain valid data");

                MetricDecimalValue result = response.data().results().stream()
                        .findFirst()
                        .flatMap(PrometheusResponse.VectorResult::getValue)
                        .map(value -> MetricDecimalValue.of(value.toString()))
                        .orElse(MetricDecimalValue.defaultValue());
                return Pair.of(query, result);
            });
        } catch (Exception exception) {
            logger.warn("Unable to read from Prometheus...", exception);
            return CompletableFuture.completedFuture(Pair.of(query, MetricDecimalValue.unavailable()));
        }
    }

    private CompletableFuture<Map<MetricsQuery, MetricDecimalValue>> getAggregatedCompletableFuture(List<MetricsQuery> queries) {
        List<CompletableFuture<Pair<MetricsQuery, MetricDecimalValue>>> futures = queries.stream()
                .map(this::readSingleMetric)
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(
                        v -> futures.stream().map(CompletableFuture::join)
                                .collect(Collectors.toMap(Pair::getKey, Pair::getValue))
                );
    }

    private PrometheusResponse queryPrometheus(MetricsQuery query) {
        URI queryUri = URI.create(prometheusUri.toString() + "/api/v1/query?query=" + encode(query.query(), UTF_8));

        ResponseEntity<PrometheusResponse> response = restTemplate.exchange(queryUri,
                HttpMethod.GET, HttpEntity.EMPTY, PrometheusResponse.class);
        return response.getBody();
    }
}
