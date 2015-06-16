package pl.allegro.tech.hermes.message.tracker.mongo;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.mongodb.DB;
import com.mongodb.DBObject;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public  class AbstractLogRepository {

    protected final MetricRegistry metricRegistry;
    protected final PathsCompiler pathsCompiler;
    protected final String clusterName;
    private final DB database;
    private final int commitInterval;
    protected BlockingQueue<DBObject> queue;

    public AbstractLogRepository(DB database, int queueSize, int commitInterval, String clusterName, MetricRegistry metricRegistry, PathsCompiler pathsCompiler) {
        this.database = database;
        this.queue = new LinkedBlockingQueue<>(queueSize);
        this.commitInterval = commitInterval;
        this.clusterName = clusterName;
        this.metricRegistry = metricRegistry;
        this.pathsCompiler = pathsCompiler;
    }

    protected void registerQueueSizeGauge(String gauge) {
        metricRegistry.register(pathsCompiler.compile(gauge), (Gauge) queue::size);
    }

    protected void registerRemainingCapacityGauge(String gauge) {
        metricRegistry.register(pathsCompiler.compile(gauge), (Gauge) queue::remainingCapacity);
    }

    protected void scheduleCommitAtFixedRate(String collectionName, String timerName) {
        Timer timer = metricRegistry.timer(pathsCompiler.compile(timerName));

        MongoQueueCommitter.scheduleCommitAtFixedRate(queue, collectionName, database, timer, commitInterval);
    }
}