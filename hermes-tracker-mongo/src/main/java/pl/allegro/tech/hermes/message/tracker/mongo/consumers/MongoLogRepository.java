package pl.allegro.tech.hermes.message.tracker.mongo.consumers;

import com.codahale.metrics.MetricRegistry;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.message.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.message.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.message.tracker.mongo.AbstractLogRepository;
import pl.allegro.tech.hermes.message.tracker.mongo.LogSchemaAware;
import pl.allegro.tech.hermes.message.tracker.mongo.metrics.Gauges;
import pl.allegro.tech.hermes.message.tracker.mongo.metrics.Timers;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.DISCARDED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.FAILED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.INFLIGHT;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.SUCCESS;

public class MongoLogRepository extends AbstractLogRepository implements LogRepository, LogSchemaAware {

    public MongoLogRepository(DB database,
                              int queueSize,
                              int commitInterval,
                              String clusterName,
                              MetricRegistry metricRegistry,
                              PathsCompiler pathsCompiler) {
        super(database, queueSize, commitInterval, clusterName, metricRegistry, pathsCompiler);

        registerQueueSizeGauge(Gauges.CONSUMER_TRACKER_QUEUE_SIZE);
        registerRemainingCapacityGauge(Gauges.CONSUMER_TRACKER_REMAINING_CAPACITY);

        scheduleCommitAtFixedRate(COLLECTION_SENT_NAME, Timers.CONSUMER_TRACKER_COMMIT_LATENCY);
    }

    @Override
    public void logSuccessful(MessageMetadata message, long timestamp, String topicName, String subscriptionName) {
        queue.offer(subscriptionLog(message, timestamp, topicName, subscriptionName, SUCCESS));
    }

    @Override
    public void logFailed(MessageMetadata message, long timestamp, String topicName, String subscriptionName, String reason) {
        queue.offer(subscriptionLog(message, timestamp, topicName, subscriptionName, FAILED).append(REASON, reason));
    }

    @Override
    public void logDiscarded(MessageMetadata message, long timestamp, String topicName, String subscriptionName, String reason) {
        queue.offer(subscriptionLog(message, timestamp, topicName, subscriptionName, DISCARDED).append(REASON, reason));
    }

    @Override
    public void logInflight(MessageMetadata message, long timestamp, String topicName, String subscriptionName) {
        queue.offer(subscriptionLog(message, timestamp, topicName, subscriptionName, INFLIGHT));
    }

    private BasicDBObject subscriptionLog(MessageMetadata message, long timestamp, String topicName, String subscriptionName,
                                          SentMessageTraceStatus status) {
        return new BasicDBObject()
                .append(MESSAGE_ID, message.getId())
                .append(TIMESTAMP, timestamp)
                .append(PUBLISH_TIMESTAMP, message.getPublishingTimestamp().orElseGet(null))
                .append(TOPIC_NAME, topicName)
                .append(SUBSCRIPTION, subscriptionName)
                .append(PARTITION, message.getPartition())
                .append(OFFSET, message.getOffset())
                .append(STATUS, status.toString())
                .append(CLUSTER, clusterName);
    }
}
