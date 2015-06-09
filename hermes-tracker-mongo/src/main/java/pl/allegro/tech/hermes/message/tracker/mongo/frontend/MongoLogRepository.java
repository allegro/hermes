package pl.allegro.tech.hermes.message.tracker.mongo.frontend;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.message.tracker.frontend.LogRepository;
import pl.allegro.tech.hermes.message.tracker.mongo.AbstractLogRepository;
import pl.allegro.tech.hermes.message.tracker.mongo.LogSchemaAware;

import java.time.Clock;
import java.util.concurrent.LinkedBlockingQueue;

import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.ERROR;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.INFLIGHT;
import static pl.allegro.tech.hermes.api.PublishedMessageTraceStatus.SUCCESS;
import static pl.allegro.tech.hermes.message.tracker.mongo.MongoQueueCommitter.scheduleCommitAtFixedRate;

public class MongoLogRepository extends AbstractLogRepository implements LogRepository, LogSchemaAware {

    private final Clock clock;
    private String clusterName;

    public MongoLogRepository(final DB database, Clock clock, int queueSize, int commitInterval,
                              String clusterName) {
        super(new LinkedBlockingQueue<>(queueSize));
        this.clock = clock;
        this.clusterName = clusterName;

        scheduleCommitAtFixedRate(queue, COLLECTION_PUBLISHED_NAME, database, commitInterval);
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
                .append(CREATED_AT, clock.millis())
                .append(TIMESTAMP, timestamp)
                .append(STATUS, status.toString())
                .append(TOPIC_NAME, topicName)
                .append(CLUSTER, clusterName);
    }
}
