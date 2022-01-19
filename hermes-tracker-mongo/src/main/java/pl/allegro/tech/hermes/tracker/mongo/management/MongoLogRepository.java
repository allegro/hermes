package pl.allegro.tech.hermes.tracker.mongo.management;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.PublishedMessageTrace;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.tracker.management.LogRepository;
import pl.allegro.tech.hermes.tracker.mongo.LogSchemaAware;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static pl.allegro.tech.hermes.api.SentMessageTrace.Builder.sentMessageTrace;

public class MongoLogRepository implements LogRepository, LogSchemaAware {

    private final DB database;

    public MongoLogRepository(DB database) {
        this.database = database;
    }

    @Override
    public List<SentMessageTrace> getLastUndeliveredMessages(String topicName,
                                                             String subscriptionName,
                                                             int limit) {
        try (
                DBCursor cursor = database.getCollection(COLLECTION_SENT_NAME).find(
                        new BasicDBObject(TOPIC_NAME, topicName)
                                .append(SUBSCRIPTION, subscriptionName)
                                .append(STATUS, SentMessageTraceStatus.DISCARDED.toString())
                ).sort(new BasicDBObject(TIMESTAMP, -1))
                        .limit(limit)
        ) {
            return StreamSupport.stream(cursor.spliterator(), false)
                    .map(this::convertToSentMessage)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<MessageTrace> getMessageStatus(String qualifiedTopicName, String subscriptionName, String messageId) {
        DBCursor publishedCursor = database.getCollection(COLLECTION_PUBLISHED_NAME)
                .find(new BasicDBObject(LogSchemaAware.MESSAGE_ID, messageId))
                .sort(new BasicDBObject(TIMESTAMP, 1));

        DBCursor sentCursor = database.getCollection(COLLECTION_SENT_NAME)
                .find(new BasicDBObject(LogSchemaAware.MESSAGE_ID, messageId).append(SUBSCRIPTION, subscriptionName))
                .sort(new BasicDBObject(TIMESTAMP, 1));

        return Stream.concat(
                    StreamSupport.stream(publishedCursor.spliterator(), false).map(this::convertToPublishedMessage),
                    StreamSupport.stream(sentCursor.spliterator(), false).map(this::convertToSentMessage))
                .collect(Collectors.toList());
    }

    private SentMessageTrace convertToSentMessage(DBObject rawObject) {
        BasicDBObject object = (BasicDBObject) rawObject;

        return sentMessageTrace(
                        object.getString(MESSAGE_ID),
                        object.getString(BATCH_ID),
                        SentMessageTraceStatus.valueOf(object.getString(STATUS))
                )
                .withTimestamp(object.getLong(TIMESTAMP))
                .withSubscription(object.getString(SUBSCRIPTION))
                .withTopicName(object.getString(TOPIC_NAME))
                .withReason(object.getString(REASON))
                .withPartition(object.getInt(PARTITION, -1))
                .withOffset(object.getLong(OFFSET, -1))
                .withCluster(object.getString(CLUSTER, ""))
                .withExtraRequestHeaders(object.getString(EXTRA_REQUEST_HEADERS))
                .build();
    }

    private PublishedMessageTrace convertToPublishedMessage(DBObject rawObject) {
        BasicDBObject object = (BasicDBObject) rawObject;

        return new PublishedMessageTrace(
                object.getString(MESSAGE_ID),
                object.getLong(TIMESTAMP),
                object.getString(TOPIC_NAME),
                PublishedMessageTraceStatus.valueOf(object.getString(STATUS)),
                object.getString(REASON),
                null,
                object.getString(CLUSTER, ""),
                object.getString(EXTRA_REQUEST_HEADERS)
        );
    }

}
