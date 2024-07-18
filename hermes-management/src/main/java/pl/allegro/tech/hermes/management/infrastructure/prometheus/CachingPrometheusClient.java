package pl.allegro.tech.hermes.management.infrastructure.prometheus;

import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.SECONDS;



public class CachingPrometheusClient implements PrometheusClient {

    private final PrometheusClient underlyingPrometheusClient;
    private final LoadingCache<List<Query>, MonitoringMetricsContainer> prometheusMetricsCache;

    public CachingPrometheusClient(PrometheusClient underlyingPrometheusClient, Ticker ticker,
                                   long cacheTtlInSeconds, long cacheSize) {
        this.underlyingPrometheusClient = underlyingPrometheusClient;
        this.prometheusMetricsCache = CacheBuilder.newBuilder()
                .ticker(ticker)
                .expireAfterWrite(cacheTtlInSeconds, SECONDS)
                .maximumSize(cacheSize)
                .build(new PrometheusMetricsCacheLoader());
    }

    @Override
    public MonitoringMetricsContainer readMetrics(List<Query> queries) {
        try {
            return prometheusMetricsCache.get(queries);
        } catch (ExecutionException e) {
            // should never happen because the loader does not throw any checked exceptions
            throw new RuntimeException(e);
        }
    }

    private class PrometheusMetricsCacheLoader extends CacheLoader<List<Query>, MonitoringMetricsContainer> {
        @Override
        public MonitoringMetricsContainer load(List<Query> queries) {
            return underlyingPrometheusClient.readMetrics(queries);
        }
    }
}