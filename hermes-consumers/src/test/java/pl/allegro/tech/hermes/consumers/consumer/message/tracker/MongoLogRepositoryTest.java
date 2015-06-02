package pl.allegro.tech.hermes.consumers.consumer.message.tracker;

import com.codahale.metrics.Timer;
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
import pl.allegro.tech.hermes.common.message.tracker.LogSchemaAware;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.consumers.consumer.message.TestMessage;
import pl.allegro.tech.hermes.consumers.message.tracker.LogRepository;
import pl.allegro.tech.hermes.consumers.message.tracker.MongoLogRepository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_SECOND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.DISCARDED;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.INFLIGHT;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.SUCCESS;

@RunWith(MockitoJUnitRunner.class)
public class MongoLogRepositoryTest implements LogSchemaAware {

    @Mock
    private Clock clock;

    @Mock
    private HermesMetrics metrics;

    private final DB database = new Fongo("trace").getDB("test");
    
    private LogRepository logRepository;

    @Before
    public void setUp() {
        when(clock.getDate()).thenReturn(new Date(1234567));
        when(metrics.timer(any(String.class))).thenReturn(new Timer());
        
        logRepository = new MongoLogRepository(database, clock, metrics, 1000, 100, "cluster");
    }

    @Test
    public void shouldLogSentMessage() throws Exception {
        // given
        String id = "sentMessage";
        String topic = "group.sentMessage";

        // when
        logRepository.logSuccessful(TestMessage.of(id), 1234L, topic, SUBSCRIPTION);

        // then
        awaitUntilMessageIsCommitted(topic, SUBSCRIPTION, id, SUCCESS);
    }

    @Test
    public void shouldLogInflightMessage() throws Exception {
        // given
        String id = "inflightMessage";
        String topic = "group.inflightMessage";

        // when
        logRepository.logInflight(TestMessage.of(id), 1234L, topic, SUBSCRIPTION);

        // then
        awaitUntilMessageIsCommitted(topic, SUBSCRIPTION, id, INFLIGHT);
    }

    @Test
    public void shouldLogUndeliveredMessage() throws Exception {
        // given
        String id = "undeliveredMessage";
        String topic = "group.undeliveredMessage";

        // when
        logRepository.logDiscarded(TestMessage.of(id), 1234L, topic, SUBSCRIPTION, "reason");

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
