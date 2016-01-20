package pl.allegro.tech.hermes.tracker.elasticsearch.management;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.PublishedMessageTrace;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.tracker.consumers.TestMessageMetadata;
import pl.allegro.tech.hermes.tracker.elasticsearch.DataInitializer;
import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchResource;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager;
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersDailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendDailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendIndexFactory;
import pl.allegro.tech.hermes.tracker.management.LogRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_MINUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.DISCARDED;

public class ElasticsearchLogRepositoryTest implements LogSchemaAware {

    private static final String CLUSTER_NAME = "primary";
    private static final String REASON_MESSAGE = "Bad Request";

    private static final Clock clock = Clock.fixed(LocalDate.of(2000, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.systemDefault());
    private static final FrontendIndexFactory frontendIndexFactory = new FrontendDailyIndexFactory(clock);
    private static final ConsumersIndexFactory consumersIndexFactory = new ConsumersDailyIndexFactory(clock);

    public static final ElasticsearchResource elasticsearch = new ElasticsearchResource(frontendIndexFactory, consumersIndexFactory);

    private DataInitializer dataInitializer;
    private LogRepository logRepository;

    @BeforeSuite
    public void before() throws Throwable {
        elasticsearch.before();
        dataInitializer = new DataInitializer(elasticsearch.client(), frontendIndexFactory, consumersIndexFactory, CLUSTER_NAME);
        SchemaManager schemaManager = new SchemaManager(elasticsearch.client(), frontendIndexFactory, consumersIndexFactory);
        logRepository = new ElasticsearchLogRepository(elasticsearch.client(), schemaManager);
    }

    @AfterSuite
    public void after() {
        elasticsearch.after();
    }

    // TODO: figure out why this test sometimes *consistently* fails on CI
    @Test(enabled = false)
    public void shouldGetLastUndelivered() throws Exception {
        //given
        String topic = "elasticsearch.lastUndelivered";
        String subscription = "subscription";
        MessageMetadata firstDiscarded = TestMessageMetadata.of("1234", topic, subscription);
        long firstTimestamp = System.currentTimeMillis();

        MessageMetadata secondDiscarded = TestMessageMetadata.of("5678", topic, subscription);
        long secondTimestamp = firstTimestamp + 1;

        // when
        dataInitializer.indexSentMessage(firstDiscarded, firstTimestamp, DISCARDED, REASON_MESSAGE);
        dataInitializer.indexSentMessage(secondDiscarded, secondTimestamp, DISCARDED, REASON_MESSAGE);

        // then
        assertThat(fetchLastUndelivered(topic, subscription)).containsExactly(sentMessageTrace(secondDiscarded, secondTimestamp, DISCARDED));
    }

    @Test
    public void shouldGetMessageStatus() throws Exception {
        //given
        MessageMetadata messageMetadata = TestMessageMetadata.of("1234", "elasticsearch.messageStatus", "subscription");
        long timestamp = System.currentTimeMillis();

        dataInitializer.indexPublishedMessage(messageMetadata, timestamp, PublishedMessageTraceStatus.SUCCESS);
        dataInitializer.indexSentMessage(messageMetadata, timestamp, SentMessageTraceStatus.SUCCESS, REASON_MESSAGE);

        //when
        assertThat(fetchMessageStatus(messageMetadata))
                .contains(publishedMessageTrace(messageMetadata, timestamp, PublishedMessageTraceStatus.SUCCESS))
                .contains(sentMessageTrace(messageMetadata, timestamp, SentMessageTraceStatus.SUCCESS));
    }

    private List<SentMessageTrace> fetchLastUndelivered(String topic, String subscription) {
        final List<SentMessageTrace> lastUndelivered = new ArrayList<>();

        await().atMost(ONE_MINUTE).until(() -> {
            lastUndelivered.clear();
            lastUndelivered.addAll(logRepository.getLastUndeliveredMessages(topic, subscription, 1));
            return lastUndelivered.size() == 1;
        });
        return lastUndelivered;
    }

    private List<MessageTrace> fetchMessageStatus(MessageMetadata messageMetadata) {
        List<MessageTrace> status = new ArrayList<>();

        await().atMost(ONE_MINUTE).until(() -> {
            status.clear();
            status.addAll(logRepository.getMessageStatus(messageMetadata.getTopic(), messageMetadata.getSubscription(), messageMetadata.getMessageId()));
            return status.size() == 2;
        });

        return status;
    }

    private SentMessageTrace sentMessageTrace(MessageMetadata messageMetadata, long timestamp, SentMessageTraceStatus status) {
        return new SentMessageTrace(messageMetadata.getMessageId(),
                messageMetadata.getBatchId(),
                timestamp,
                messageMetadata.getSubscription(),
                messageMetadata.getTopic(),
                status,
                REASON_MESSAGE,
                null,
                messageMetadata.getPartition(),
                messageMetadata.getOffset(),
                CLUSTER_NAME);
    }

    private PublishedMessageTrace publishedMessageTrace(MessageMetadata messageMetadata, long timestamp, PublishedMessageTraceStatus status) {
        return new PublishedMessageTrace(messageMetadata.getMessageId(),
                timestamp,
                messageMetadata.getTopic(),
                status,
                null,
                null,
                CLUSTER_NAME);
    }
}