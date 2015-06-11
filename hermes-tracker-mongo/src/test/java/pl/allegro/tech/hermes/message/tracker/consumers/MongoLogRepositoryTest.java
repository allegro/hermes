package pl.allegro.tech.hermes.message.tracker.consumers;

import com.codahale.metrics.MetricRegistry;
import com.github.fakemongo.Fongo;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.message.tracker.mongo.LogSchemaAware;
import pl.allegro.tech.hermes.message.tracker.mongo.consumers.MongoLogRepository;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_SECOND;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.DISCARDED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.INFLIGHT;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.SUCCESS;

@RunWith(MockitoJUnitRunner.class)
public class MongoLogRepositoryTest implements LogSchemaAware {

    @Mock
    private Clock clock;

    private final DB database = new Fongo("trace").getDB("test");
    
    private LogRepository logRepository;

    @Before
    public void setUp() {
        logRepository = new MongoLogRepository(database, 1000, 100, "cluster", new MetricRegistry(), new PathsCompiler("localhost"));
    }

    @Test
    public void shouldLogSentMessage() throws Exception {
        // given
        String id = "sentMessage";
        String topic = "group.sentMessage";

        // when
        logRepository.logSuccessful(TestMessageMetadata.of(id), 1234L, topic, SUBSCRIPTION);

        // then
        awaitUntilMessageIsCommitted(topic, SUBSCRIPTION, id, SUCCESS);
    }

    @Test
    public void shouldLogInflightMessage() throws Exception {
        // given
        String id = "inflightMessage";
        String topic = "group.inflightMessage";

        // when
        logRepository.logInflight(TestMessageMetadata.of(id), 1234L, topic, SUBSCRIPTION);

        // then
        awaitUntilMessageIsCommitted(topic, SUBSCRIPTION, id, INFLIGHT);
    }

    @Test
    public void shouldLogUndeliveredMessage() throws Exception {
        // given
        String id = "undeliveredMessage";
        String topic = "group.undeliveredMessage";

        // when
        logRepository.logDiscarded(TestMessageMetadata.of(id), 1234L, topic, SUBSCRIPTION, "reason");

        // then
        awaitUntilMessageIsCommitted(topic, SUBSCRIPTION, id, DISCARDED);
    }

    private void awaitUntilMessageIsCommitted(String topic, String subscription, String messageId, SentMessageTraceStatus status) {
        await().atMost(ONE_SECOND).until(() -> {
            List<SentMessageTrace> messages = getLastUndeliveredMessages(topic, subscription, status);
            assertThat(messages).hasSize(1).extracting(MESSAGE_ID).containsExactly(messageId);
        });
    }

    public List<SentMessageTrace> getLastUndeliveredMessages(String topicName, String subscriptionName, SentMessageTraceStatus status) {
        try (
                DBCursor cursor = database.getCollection(COLLECTION_SENT_NAME)
                        .find(new BasicDBObject(TOPIC_NAME, topicName)
                                .append(SUBSCRIPTION, subscriptionName).append(STATUS, status.toString()))
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
                object.getLong(TIMESTAMP),
                object.getString(SUBSCRIPTION),
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
