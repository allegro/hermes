package pl.allegro.tech.hermes.tracker.mongo.consumers;

import com.codahale.metrics.MetricRegistry;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.tracker.BatchingLogRepository;
import pl.allegro.tech.hermes.tracker.mongo.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.mongo.MongoQueueCommitter;
import pl.allegro.tech.hermes.tracker.mongo.metrics.Gauges;
import pl.allegro.tech.hermes.tracker.mongo.metrics.Timers;

import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.*;

public class MongoLogRepository extends BatchingLogRepository<DBObject> implements LogRepository, LogSchemaAware {

    public MongoLogRepository(DB database,
                              int queueSize,
                              int commitInterval,
                              String clusterName,
                              MetricRegistry metricRegistry,
                              PathsCompiler pathsCompiler) {
        super(queueSize, clusterName, metricRegistry, pathsCompiler);

        registerQueueSizeGauge(Gauges.CONSUMER_TRACKER_MONGO_QUEUE_SIZE);
        registerRemainingCapacityGauge(Gauges.CONSUMER_TRACKER_MONGO_REMAINING_CAPACITY);

        MongoQueueCommitter.scheduleCommitAtFixedRate(queue, COLLECTION_SENT_NAME, database,
                metricRegistry.timer(pathsCompiler.compile(Timers.CONSUMER_TRACKER_MONGO_COMMIT_LATENCY)), commitInterval);
    }

    @Override
    public void logSuccessful(MessageMetadata message, long timestamp) {
        queue.offer(subscriptionLog(message, timestamp, SUCCESS));
    }

    @Override
    public void logFailed(MessageMetadata message, long timestamp, String reason) {
        queue.offer(subscriptionLog(message, timestamp, FAILED).append(REASON, reason));
    }

    @Override
    public void logDiscarded(MessageMetadata message, long timestamp, String reason) {
        queue.offer(subscriptionLog(message, timestamp, DISCARDED).append(REASON, reason));
    }

    @Override
    public void logInflight(MessageMetadata message, long timestamp) {
        queue.offer(subscriptionLog(message, timestamp, INFLIGHT));
    }

    private BasicDBObject subscriptionLog(MessageMetadata message, long timestamp, SentMessageTraceStatus status) {
        return new BasicDBObject()
                .append(MESSAGE_ID, message.getMessageId())
                .append(BATCH_ID, message.getBatchId())
                .append(TIMESTAMP, timestamp)
                .append(PUBLISH_TIMESTAMP, message.getPublishingTimestamp())
                .append(TOPIC_NAME, message.getTopic())
                .append(SUBSCRIPTION, message.getSubscription())
                .append(PARTITION, message.getPartition())
                .append(OFFSET, message.getOffset())
                .append(STATUS, status.toString())
                .append(CLUSTER, clusterName);
    }
}
