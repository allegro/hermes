package pl.allegro.tech.hermes.consumers.message.tracker;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.message.tracker.LogSchemaAware;
import pl.allegro.tech.hermes.common.metric.Gauges;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Timers;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

import javax.inject.Inject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.DISCARDED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.FAILED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.INFLIGHT;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.SUCCESS;
import static pl.allegro.tech.hermes.common.config.Configs.TRACKER_MONGODB_COMMIT_INTERVAL;
import static pl.allegro.tech.hermes.common.config.Configs.TRACKER_MONGODB_QUEUE_CAPACITY;
import static pl.allegro.tech.hermes.common.message.tracker.mongo.MongoQueueCommitter.scheduleCommitAtFixedRate;

public class MongoLogRepository implements LogRepository, LogSchemaAware {

    private final Clock clock;
    private final BlockingQueue<DBObject> queue;
    private final String clusterName;

    @Inject
    public MongoLogRepository(final DB database, Clock clock, HermesMetrics metrics, ConfigFactory config) {
        this(database, clock, metrics,
                config.getIntProperty(TRACKER_MONGODB_QUEUE_CAPACITY),
                config.getIntProperty(TRACKER_MONGODB_COMMIT_INTERVAL),
                config.getStringProperty(Configs.KAFKA_CLUSTER_NAME));
    }

    public MongoLogRepository(final DB database, Clock clock, HermesMetrics metrics, int queueSize, int commitInterval,
                              String clusterName) {
        this.clock = clock;
        this.queue = new LinkedBlockingQueue<>(queueSize);
        this.clusterName = clusterName;

        metrics.registerGauge(Gauges.CONSUMER_TRACKER_QUEUE_SIZE, () -> queue.size());
        metrics.registerGauge(Gauges.CONSUMER_TRACKER_REMAINING_CAPACITY, () -> queue.remainingCapacity());

        scheduleCommitAtFixedRate(queue, COLLECTION_SENT_NAME, database,
                metrics.timer(Timers.CONSUMER_TRACKER_COMMIT_LATENCY), commitInterval);
    }

    @Override
    public void logSuccessful(Message message, long timestamp, String topicName, String subscriptionName) {
        queue.offer(subscriptionLog(message, timestamp, topicName, subscriptionName, SUCCESS));
    }

    @Override
    public void logFailed(Message message, long timestamp, String topicName, String subscriptionName, String reason) {
        queue.offer(subscriptionLog(message, timestamp, topicName, subscriptionName, FAILED).append(REASON, reason));
    }

    @Override
    public void logDiscarded(Message message, long timestamp, String topicName, String subscriptionName, String reason) {
        queue.offer(subscriptionLog(message, timestamp, topicName, subscriptionName, DISCARDED).append(REASON, reason));
    }

    @Override
    public void logInflight(Message message, long timestamp, String topicName, String subscriptionName) {
        queue.offer(subscriptionLog(message, timestamp, topicName, subscriptionName, INFLIGHT));
    }

    private BasicDBObject subscriptionLog(Message message, long timestamp, String topicName, String subscriptionName,
                                          SentMessageTraceStatus status) {
        return new BasicDBObject()
                .append(MESSAGE_ID, message.getId().get())
                .append(CREATED_AT, clock.getDate())
                .append(TIMESTAMP, timestamp)
                .append(TOPIC_NAME, topicName)
                .append(SUBSCRIPTION, subscriptionName)
                .append(PARTITION, message.getPartition())
                .append(OFFSET, message.getOffset())
                .append(STATUS, status.toString())
                .append(CLUSTER, clusterName);
    }
}
