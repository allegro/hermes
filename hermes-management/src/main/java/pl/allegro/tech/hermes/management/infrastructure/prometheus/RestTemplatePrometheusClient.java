package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.api.MetricDecimalValue;
import pl.allegro.tech.hermes.api.MetricHistogramValue;
import pl.allegro.tech.hermes.api.MetricUnavailable;
import pl.allegro.tech.hermes.api.MetricValue;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;

public class RestTemplatePrometheusClient implements PrometheusClient {

  private static final Logger logger = LoggerFactory.getLogger(RestTemplatePrometheusClient.class);

  private final URI prometheusUri;
  private final RestTemplate restTemplate;
  private final ExecutorService executorService;
  private final Duration fetchingTimeout;
  private final MeterRegistry meterRegistry;

  public RestTemplatePrometheusClient(
      RestTemplate restTemplate,
      URI prometheusUri,
      ExecutorService executorService,
      Duration fetchingTimeoutMillis,
      MeterRegistry meterRegistry) {
    this.restTemplate = restTemplate;
    this.prometheusUri = prometheusUri;
    this.executorService = executorService;
    this.fetchingTimeout = fetchingTimeoutMillis;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public MonitoringMetricsContainer readMetrics(List<String> queries) {
    return fetchInParallelFromPrometheus(queries);
  }

  private MonitoringMetricsContainer fetchInParallelFromPrometheus(List<String> queries) {
    CompletableFuture<Map<String, MetricValue>> aggregatedFuture =
        getAggregatedCompletableFuture(queries);

    try {
      Map<String, MetricValue> metrics =
          aggregatedFuture.get(fetchingTimeout.toMillis(), TimeUnit.MILLISECONDS);
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

  private CompletableFuture<Map<String, MetricValue>> getAggregatedCompletableFuture(
      List<String> queries) {
    // has to be collected to run in parallel
    List<CompletableFuture<Pair<String, MetricValue>>> futures =
        queries.stream().map(this::readSingleMetric).toList();

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(
            v ->
                futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
  }

  private CompletableFuture<Pair<String, MetricValue>> readSingleMetric(String query) {
    return CompletableFuture.supplyAsync(() -> queryPrometheus(query), executorService);
  }

  private Pair<String, MetricValue> queryPrometheus(String query) {
    try {
      URI queryUri =
          URI.create(prometheusUri.toString() + "/api/v1/query?query=" + encode(query, UTF_8));
      PrometheusResponse response =
          restTemplate
              .exchange(queryUri, HttpMethod.GET, HttpEntity.EMPTY, PrometheusResponse.class)
              .getBody();

      Preconditions.checkNotNull(response, "Prometheus response is null");
      Preconditions.checkState(
          response.isSuccess(), "Prometheus response does not contain valid data");

      MetricValue result = parseResponse(response);
      meterRegistry.counter("read-metric-from-prometheus.success").increment();
      return Pair.of(query, result);
    } catch (HttpStatusCodeException ex) {
      logger.warn(
          "Unable to read from Prometheus. Query: {}, Status code: {}. Response body: {}",
          query,
          ex.getStatusCode(),
          ex.getResponseBodyAsString(),
          ex);
      return Pair.of(query, MetricUnavailable.INSTANCE);
    } catch (Exception ex) {
      logger.warn("Unable to read from Prometheus. Query: {}", query, ex);
      meterRegistry.counter("read-metric-from-prometheus.error").increment();
      return Pair.of(query, MetricUnavailable.INSTANCE);
    }
  }

  private MetricValue parseResponse(PrometheusResponse response) {
    List<PrometheusResponse.VectorResult> results = response.data().results();

    boolean isHistogram = response.data().isHistogram();
    return isHistogram ? parseHistogram(results) : parseDecimal(results);
  }

  private static MetricHistogramValue parseHistogram(
      List<PrometheusResponse.VectorResult> results) {
    Map<String, String> buckets =
        results.stream()
            .collect(
                Collectors.toMap(
                    vectorResult -> vectorResult.metric().le(),
                    vectorResult ->
                        vectorResult
                            .getLongValue()
                            .map(Object::toString)
                            .orElse(MetricHistogramValue.defaultBucketValue())));
    return buckets.isEmpty()
        ? MetricHistogramValue.defaultValue()
        : MetricHistogramValue.ofBuckets(buckets);
  }

  private static MetricDecimalValue parseDecimal(List<PrometheusResponse.VectorResult> results) {
    return results.stream()
        .findFirst()
        .flatMap(PrometheusResponse.VectorResult::getDoubleValue)
        .map(value -> MetricDecimalValue.of(value.toString()))
        .orElse(MetricDecimalValue.defaultValue());
  }
}
