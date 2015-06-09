package pl.allegro.tech.hermes.message.tracker.mongo.consumers;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.message.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.message.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.message.tracker.mongo.AbstractLogRepository;
import pl.allegro.tech.hermes.message.tracker.mongo.LogSchemaAware;

import java.time.Clock;
import java.util.concurrent.LinkedBlockingQueue;

import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.DISCARDED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.FAILED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.INFLIGHT;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.SUCCESS;
import static pl.allegro.tech.hermes.message.tracker.mongo.MongoQueueCommitter.scheduleCommitAtFixedRate;

public class MongoLogRepository extends AbstractLogRepository implements LogRepository, LogSchemaAware {

    private final Clock clock;
    private final String clusterName;

    public MongoLogRepository(final DB database, Clock clock, int queueSize, int commitInterval,
                              String clusterName) {
        super(new LinkedBlockingQueue<>(queueSize));
        this.clock = clock;
        this.clusterName = clusterName;

        scheduleCommitAtFixedRate(queue, COLLECTION_SENT_NAME, database, commitInterval);
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
                .append(MESSAGE_ID, message.getId().get())
                .append(CREATED_AT, clock.millis())
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
