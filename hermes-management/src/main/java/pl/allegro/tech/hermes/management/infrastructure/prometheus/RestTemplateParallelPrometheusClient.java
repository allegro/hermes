package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.MeterRegistry;
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
    private final MeterRegistry meterRegistry;


    public RestTemplateParallelPrometheusClient(RestTemplate restTemplate,
                                                URI prometheusUri,
                                                ExecutorService executorService,
                                                Duration parallelFetchingTimeoutMillis, MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.prometheusUri = prometheusUri;
        this.executorService = executorService;
        this.parallelFetchingTimeout = parallelFetchingTimeoutMillis;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public MonitoringMetricsContainer readMetrics(List<MetricsQuery> queries) {
        return fetchInParallelFromPrometheus(queries);
    }

    private MonitoringMetricsContainer fetchInParallelFromPrometheus(List<MetricsQuery> queries) {
        CompletableFuture<Map<MetricsQuery, MetricDecimalValue>> aggregatedFuture = getAggregatedCompletableFuture(queries);

        try {
            Map<MetricsQuery, MetricDecimalValue> metrics = aggregatedFuture.get(parallelFetchingTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return MonitoringMetricsContainer.initialized(metrics);
        } catch (InterruptedException e) {
            // possibly let know the caller that the thread was interrupted
            Thread.currentThread().interrupt();
            logger.warn("Prometheus fetching thread was interrupted...", e);
            return MonitoringMetricsContainer.unavailable();
        } catch (Exception ex) {
            logger.warn("Unexpected exception during fetching metrics from prometheus...", ex);
            return MonitoringMetricsContainer.unavailable();
        }
    }

    private CompletableFuture<Map<MetricsQuery, MetricDecimalValue>> getAggregatedCompletableFuture(List<MetricsQuery> queries) {
        // has to be collected to run in parallel
        List<CompletableFuture<Pair<MetricsQuery, MetricDecimalValue>>> futures = queries.stream()
                .map(this::readSingleMetric)
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(
                        v -> futures.stream().map(CompletableFuture::join)
                                .collect(Collectors.toMap(Pair::getKey, Pair::getValue))
                );
    }

    private CompletableFuture<Pair<MetricsQuery, MetricDecimalValue>> readSingleMetric(MetricsQuery query) {
        return CompletableFuture.supplyAsync(() -> queryPrometheus(query), executorService);
    }

    private Pair<MetricsQuery, MetricDecimalValue> queryPrometheus(MetricsQuery query) {
        try {
            URI queryUri = URI.create(prometheusUri.toString() + "/api/v1/query?query=" + encode(query.query(), UTF_8));
            PrometheusResponse response = restTemplate.exchange(queryUri,
                    HttpMethod.GET, HttpEntity.EMPTY, PrometheusResponse.class).getBody();

            Preconditions.checkNotNull(response, "Prometheus response is null");
            Preconditions.checkState(response.isSuccess(), "Prometheus response does not contain valid data");

            MetricDecimalValue result = parseResponse(response);
            meterRegistry.counter("read-metric-from-prometheus.success").increment();
            logger.warn("Successfull read query from Prometheus...");
            return Pair.of(query, result);
        } catch (Exception exception) {
            logger.warn("Unable to read from Prometheus...", exception);
            meterRegistry.counter("read-metric-from-prometheus.error").increment();
            return Pair.of(query, MetricDecimalValue.unavailable());
        }
    }

    private MetricDecimalValue parseResponse(PrometheusResponse response) {
        return response.data().results().stream()
                .findFirst()
                .flatMap(PrometheusResponse.VectorResult::getValue)
                .map(value -> MetricDecimalValue.of(value.toString()))
                .orElse(MetricDecimalValue.defaultValue());
    }
}
