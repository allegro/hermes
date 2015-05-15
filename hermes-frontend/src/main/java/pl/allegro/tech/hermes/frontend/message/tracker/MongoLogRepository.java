package pl.allegro.tech.hermes.frontend.message.tracker;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.message.tracker.LogSchemaAware;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.time.Clock;

import javax.inject.Inject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.ERROR;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.SUCCESS;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.INFLIGHT;
import static pl.allegro.tech.hermes.common.message.tracker.QueueMetrics.registerCurrentSizeGauge;
import static pl.allegro.tech.hermes.common.message.tracker.QueueMetrics.registerRemainingCapacityGauge;
import static pl.allegro.tech.hermes.common.message.tracker.mongo.MongoQueueCommitter.scheduleCommitAtFixedRate;
import static pl.allegro.tech.hermes.common.metric.Metrics.Gauge.PRODUCER_TRACKER_QUEUE_SIZE;
import static pl.allegro.tech.hermes.common.metric.Metrics.Gauge.PRODUCER_TRACKER_REMAINING_CAPACITY;
import static pl.allegro.tech.hermes.common.metric.Metrics.Timer.PRODUCER_TRACKER_COMMIT_LATENCY;

public class MongoLogRepository implements LogRepository, LogSchemaAware {

    private final Clock clock;
    private BlockingQueue<DBObject> queue;
    private String clusterName;

    @Inject
    public MongoLogRepository(final DB database, Clock clock, HermesMetrics metrics, ConfigFactory config) {
        this(database, clock, metrics,
                config.getIntProperty(Configs.TRACKER_MONGODB_QUEUE_CAPACITY),
                config.getIntProperty(Configs.TRACKER_MONGODB_COMMIT_INTERVAL),
                config.getStringProperty(Configs.KAFKA_CLUSTER_NAME));
    }

    public MongoLogRepository(final DB database, Clock clock, HermesMetrics metrics, int queueSize, int commitInterval,
                              String clusterName) {
        this.clock = clock;
        this.queue = new LinkedBlockingQueue<>(queueSize);
        this.clusterName = clusterName;
        registerCurrentSizeGauge(queue, PRODUCER_TRACKER_QUEUE_SIZE, metrics);
        registerRemainingCapacityGauge(queue, PRODUCER_TRACKER_REMAINING_CAPACITY, metrics);
        scheduleCommitAtFixedRate(queue, COLLECTION_PUBLISHED_NAME, database,
                metrics.timer(PRODUCER_TRACKER_COMMIT_LATENCY), commitInterval);
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
                .append(CREATED_AT, clock.getDate())
                .append(TIMESTAMP, timestamp)
                .append(STATUS, status.toString())
                .append(TOPIC_NAME, topicName)
                .append(CLUSTER, clusterName);
    }
}
