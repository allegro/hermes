package pl.allegro.tech.hermes.management.infrastructure.graphite;

import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import pl.allegro.tech.hermes.api.MetricDecimalValue;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer;

import static com.google.common.collect.Iterables.toArray;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class CachingGraphiteClient implements GraphiteClient {

    private final GraphiteClient underlyingGraphiteClient;
    private final LoadingCache<String, MetricDecimalValue> graphiteMetricsCache;

    public CachingGraphiteClient(GraphiteClient underlyingGraphiteClient, Ticker ticker, long cacheTtlInSeconds, long cacheSize) {
        this.underlyingGraphiteClient = underlyingGraphiteClient;
        this.graphiteMetricsCache = CacheBuilder.newBuilder()
                .ticker(ticker)
                .expireAfterWrite(cacheTtlInSeconds, SECONDS)
                .maximumSize(cacheSize)
                .build(new GraphiteMetricsCacheLoader());
    }

    @Override
    public MonitoringMetricsContainer readMetrics(String... metricPaths) {
        try {
            Map<String, MetricDecimalValue> graphiteMetrics = graphiteMetricsCache.getAll(asList(metricPaths));
            return new MonitoringMetricsContainer(graphiteMetrics);
        } catch (ExecutionException e) {
            // should never happen because the loader does not throw any checked exceptions
            throw new RuntimeException(e);
        }
    }

    private class GraphiteMetricsCacheLoader extends CacheLoader<String, MetricDecimalValue> {
        @Override
        public MetricDecimalValue load(String metricPath) {
            return loadAll(singleton(metricPath)).get(metricPath);
        }

        @Override
        public Map<String, MetricDecimalValue> loadAll(Iterable<? extends String> metricPaths) {
            String[] metricPathsArray = toArray(metricPaths, String.class);
            MonitoringMetricsContainer metricsContainer = underlyingGraphiteClient.readMetrics(metricPathsArray);
            return stream(metricPathsArray).collect(toMap(
                    identity(),
                    metricsContainer::metricValue
            ));
        }
    }
}