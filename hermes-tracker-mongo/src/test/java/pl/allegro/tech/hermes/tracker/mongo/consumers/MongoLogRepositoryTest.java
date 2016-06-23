package pl.allegro.tech.hermes.tracker.mongo.consumers;

import com.codahale.metrics.MetricRegistry;
import com.github.fakemongo.Fongo;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.tracker.consumers.AbstractLogRepositoryTest;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.mongo.LogSchemaAware;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_SECOND;
import static org.assertj.core.api.Assertions.assertThat;

public class MongoLogRepositoryTest extends AbstractLogRepositoryTest implements LogSchemaAware {

    private final DB database = new Fongo("trace").getDB("test");

    @Override
    protected LogRepository createLogRepository() {
        return new MongoLogRepository(database, 1000, 100, "cluster", new MetricRegistry(), new PathsCompiler("localhost"));
    }

    protected void awaitUntilMessageIsPersisted(String topic, String subscription, String messageId, SentMessageTraceStatus status) {
        await().atMost(ONE_SECOND).until(() -> {
            List<SentMessageTrace> messages = getLastUndeliveredMessages(topic, subscription, status);
            assertThat(messages).hasSize(1).extracting(MESSAGE_ID).containsExactly(messageId);
        });
    }

    @Override
    protected void awaitUntilBatchMessageIsPersisted(String topic, String subscription, String messageId, String batchId, SentMessageTraceStatus status) throws Exception {
        await().atMost(ONE_SECOND).until(() -> {
            List<SentMessageTrace> messages = getLastUndeliveredMessages(topic, subscription, status);
            assertThat(messages).hasSize(1).extracting(MESSAGE_ID).containsExactly(messageId);
            assertThat(messages).hasSize(1).extracting(BATCH_ID).containsExactly(batchId);
        });
    }

    private List<SentMessageTrace> getLastUndeliveredMessages(String topicName, String subscriptionName, SentMessageTraceStatus status) {
        try (
                DBCursor cursor = database.getCollection(COLLECTION_SENT_NAME)
                        .find(new BasicDBObject(TOPIC_NAME, topicName)
                                .append(LogSchemaAware.SUBSCRIPTION, subscriptionName).append(STATUS, status.toString()))
                        .sort(new BasicDBObject(TIMESTAMP, -1)).limit(1)
        ) {
            return StreamSupport.stream(cursor.spliterator(), false)
                    .map(this::convert)
                    .collect(Collectors.toList());
        }
    }

    private SentMessageTrace convert(DBObject rawObject) {
        BasicDBObject object = (BasicDBObject) rawObject;
        return new SentMessageTrace(
                object.getString(MESSAGE_ID),
                object.getString(BATCH_ID),
                object.getLong(TIMESTAMP),
                object.getString(LogSchemaAware.SUBSCRIPTION),
                object.getString(TOPIC_NAME),
                SentMessageTraceStatus.valueOf(object.getString(STATUS)),
                object.getString(REASON),
                null,
                object.getInt(PARTITION, -1),
                object.getLong(OFFSET, -1),
                object.getString(CLUSTER, "")
        );
    }
}
