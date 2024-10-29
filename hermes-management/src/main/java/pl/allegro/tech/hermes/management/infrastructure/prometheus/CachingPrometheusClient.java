package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.concurrent.ExecutionException;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;

public class CachingPrometheusClient implements PrometheusClient {

  private final PrometheusClient underlyingPrometheusClient;
  /*
   Metrics will always be requested in the context of a single subscription/topic. The single sub/topic will
   always result in the same list of metrics queries. There is no overlapping between metrics used in the context of
   topic or subscriptions. That's why it is safe to use a list of queries as a caching key.

   Maybe it will be worth to cache it per query except of queries when there will be too much overhead
   of refreshing all sub/topic metrics if the single fetch fails (currently we invalidate whole metrics container
   when one of the sub metric is unavailable)
  */
  private final LoadingCache<List<String>, MonitoringMetricsContainer> prometheusMetricsCache;

  public CachingPrometheusClient(
      PrometheusClient underlyingPrometheusClient,
      Ticker ticker,
      long cacheTtlInSeconds,
      long cacheSize) {
    this.underlyingPrometheusClient = underlyingPrometheusClient;
    this.prometheusMetricsCache =
        CacheBuilder.newBuilder()
            .ticker(ticker)
            .expireAfterWrite(cacheTtlInSeconds, SECONDS)
            .maximumSize(cacheSize)
            .build(new PrometheusMetricsCacheLoader());
  }

  @Override
  public MonitoringMetricsContainer readMetrics(List<String> queries) {
    try {
      MonitoringMetricsContainer monitoringMetricsContainer =
          prometheusMetricsCache.get(List.copyOf(queries));
      if (monitoringMetricsContainer.hasUnavailableMetrics()) {
        // try to reload the on the next fetch
        prometheusMetricsCache.invalidate(queries);
      }
      return monitoringMetricsContainer;
    } catch (ExecutionException e) {
      // should never happen because the loader does not throw any exceptions
      throw new RuntimeException(e);
    }
  }

  private class PrometheusMetricsCacheLoader
      extends CacheLoader<List<String>, MonitoringMetricsContainer> {
    @Override
    public MonitoringMetricsContainer load(List<String> queries) {
      return underlyingPrometheusClient.readMetrics(queries);
    }
  }
}
