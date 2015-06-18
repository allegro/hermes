package pl.allegro.tech.hermes.tracker.mongo.frontend;

import com.codahale.metrics.MetricRegistry;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.tracker.frontend.LogRepository;
import pl.allegro.tech.hermes.tracker.mongo.AbstractLogRepository;
import pl.allegro.tech.hermes.tracker.mongo.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.mongo.metrics.Gauges;
import pl.allegro.tech.hermes.tracker.mongo.metrics.Timers;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.*;

public class MongoLogRepository extends AbstractLogRepository implements LogRepository, LogSchemaAware {

    public MongoLogRepository(DB database,
                              int queueSize,
                              int commitIntervalMs,
                              String clusterName,
                              MetricRegistry metricRegistry,
                              PathsCompiler pathsCompiler) {
        super(database, queueSize, commitIntervalMs, clusterName, metricRegistry, pathsCompiler);

        registerQueueSizeGauge(Gauges.PRODUCER_TRACKER_QUEUE_SIZE);
        registerRemainingCapacityGauge(Gauges.PRODUCER_TRACKER_REMAINING_CAPACITY);

        scheduleCommitAtFixedRate(COLLECTION_PUBLISHED_NAME, Timers.PRODUCER_TRACKER_COMMIT_LATENCY);
    }

    @Override
    public void logPublished(String messageId, long timestamp, String topicName) {
        queue.offer(topicLog(messageId, timestamp, topicName, SUCCESS));
    }

    @Override
    public void logError(String messageId, long timestamp, String topicName, String reason) {
        queue.offer(topicLog(messageId, timestamp, topicName, ERROR).append(REASON, reason));
    }

    @Override
    public void logInflight(String messageId, long timestamp, String topicName) {
        queue.offer(topicLog(messageId, timestamp, topicName, INFLIGHT));
    }

    private BasicDBObject topicLog(String messageId, long timestamp, String topicName, PublishedMessageTraceStatus status) {
        return new BasicDBObject()
                .append(MESSAGE_ID, messageId)
                .append(TIMESTAMP, timestamp)
                .append(STATUS, status.toString())
                .append(TOPIC_NAME, topicName)
                .append(CLUSTER, clusterName);
    }
}
