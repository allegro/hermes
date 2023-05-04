package pl.allegro.tech.hermes.tracker;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import pl.allegro.tech.hermes.metrics.MetricRegistryPathsCompiler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BatchingLogRepository<T> {

    protected final MetricRegistry metricRegistry;
    protected final MetricRegistryPathsCompiler pathsCompiler;
    protected final String clusterName;
    protected final String hostname;
    protected BlockingQueue<T> queue;

    public BatchingLogRepository(int queueSize,
                                 String clusterName,
                                 String hostname,
                                 MetricRegistry metricRegistry,
                                 MetricRegistryPathsCompiler pathsCompiler) {
        this.queue = new LinkedBlockingQueue<>(queueSize);
        this.clusterName = clusterName;
        this.hostname = hostname;
        this.metricRegistry = metricRegistry;
        this.pathsCompiler = pathsCompiler;
    }

    protected void registerQueueSizeGauge(String gauge) {
        metricRegistry.register(pathsCompiler.compile(gauge), (Gauge) queue::size);
    }

    protected void registerRemainingCapacityGauge(String gauge) {
        metricRegistry.register(pathsCompiler.compile(gauge), (Gauge) queue::remainingCapacity);
    }

}