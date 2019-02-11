package pl.allegro.tech.hermes.tracker.elasticsearch.management;

import com.codahale.metrics.MetricRegistry;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.PublishedMessageTrace;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.tracker.consumers.TestMessageMetadata;
import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchResource;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager;
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersDailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersElasticsearchLogRepository;
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendDailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendElasticsearchLogRepository;
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

    private static final Clock clock = Clock.fixed(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.systemDefault());
    private static final FrontendIndexFactory frontendIndexFactory = new FrontendDailyIndexFactory(clock);
    private static final ConsumersIndexFactory consumersIndexFactory = new ConsumersDailyIndexFactory(clock);

    private static final ElasticsearchResource elasticsearch = new ElasticsearchResource();

    private LogRepository logRepository;
    private FrontendElasticsearchLogRepository frontendLogRepository;
    private ConsumersElasticsearchLogRepository consumersLogRepository;

    @BeforeSuite
    public void before() throws Throwable {
        elasticsearch.before();
        SchemaManager schemaManager = new SchemaManager(elasticsearch.client(), frontendIndexFactory, consumersIndexFactory, false);
        logRepository = new ElasticsearchLogRepository(elasticsearch.client(), schemaManager);

        PathsCompiler pathsCompiler = new PathsCompiler("localhost");
        MetricRegistry metricRegistry = new MetricRegistry();
        frontendLogRepository = new FrontendElasticsearchLogRepository.Builder(
                elasticsearch.client(), pathsCompiler, metricRegistry)
                .withIndexFactory(frontendIndexFactory)
                .build();

        consumersLogRepository = new ConsumersElasticsearchLogRepository.Builder(
                elasticsearch.client(), pathsCompiler, metricRegistry)
                .withIndexFactory(consumersIndexFactory)
                .build();
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
        // difference between first and second timestamp must be bigger than 1000ms as query sorts by seconds
        long secondTimestamp = firstTimestamp + 2000;

        // when
        consumersLogRepository.logDiscarded(firstDiscarded, firstTimestamp, REASON_MESSAGE);
        consumersLogRepository.logDiscarded(secondDiscarded, secondTimestamp, REASON_MESSAGE);

        // then
        assertThat(fetchLastUndelivered(topic, subscription)).containsExactly(sentMessageTrace(secondDiscarded, secondTimestamp, DISCARDED));
    }

    @Test
    public void shouldGetMessageStatus() {
        //given
        MessageMetadata messageMetadata = TestMessageMetadata.of("1234", "elasticsearch.messageStatus", "subscription");
        long timestamp = System.currentTimeMillis();

        frontendLogRepository.logPublished("1234", timestamp, "elasticsearch.messageStatus", "localhost");
        consumersLogRepository.logSuccessful(messageMetadata, "localhost", timestamp);

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
            status.addAll(logRepository.getMessageStatus(messageMetadata.getTopic(), messageMetadata.getSubscription(),
                    messageMetadata.getMessageId()));
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