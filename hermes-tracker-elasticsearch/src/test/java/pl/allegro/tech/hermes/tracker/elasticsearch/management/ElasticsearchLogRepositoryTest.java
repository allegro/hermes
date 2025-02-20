package pl.allegro.tech.hermes.tracker.elasticsearch.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.api.SentMessageTraceStatus.DISCARDED;
import static pl.allegro.tech.hermes.common.http.ExtraRequestHeadersCollector.extraRequestHeadersCollector;

import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.PublishedMessageTrace;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
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

public class ElasticsearchLogRepositoryTest implements LogSchemaAware {

  private static final String CLUSTER_NAME = "primary";
  private static final String REASON_MESSAGE = "Bad Request";

  private static final Clock clock =
      Clock.fixed(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.systemDefault());
  private static final FrontendIndexFactory frontendIndexFactory =
      new FrontendDailyIndexFactory(clock);
  private static final ConsumersIndexFactory consumersIndexFactory =
      new ConsumersDailyIndexFactory(clock);
  private static final MetricsFacade metricsFacade = new MetricsFacade(new SimpleMeterRegistry());

  private static final ElasticsearchResource elasticsearch = new ElasticsearchResource();

  private LogRepository logRepository;
  private FrontendElasticsearchLogRepository frontendLogRepository;
  private ConsumersElasticsearchLogRepository consumersLogRepository;

  @BeforeClass
  public static void beforeAll() throws Throwable {
    elasticsearch.before();
  }

  @AfterClass
  public static void afterAll() {
    elasticsearch.after();
  }

  @Before
  public void setUp() {
    SchemaManager schemaManager =
        new SchemaManager(
            elasticsearch.client(), frontendIndexFactory, consumersIndexFactory, false);
    logRepository = new ElasticsearchLogRepository(elasticsearch.client(), schemaManager);

    frontendLogRepository =
        new FrontendElasticsearchLogRepository.Builder(elasticsearch.client(), metricsFacade)
            .withIndexFactory(frontendIndexFactory)
            .build();

    consumersLogRepository =
        new ConsumersElasticsearchLogRepository.Builder(elasticsearch.client(), metricsFacade)
            .withIndexFactory(consumersIndexFactory)
            .build();
  }

  // TODO: figure out why this test sometimes *consistently* fails on CI
  @Ignore
  @Test
  public void shouldGetLastUndelivered() throws Exception {
    // given
    String topic = "elasticsearch.lastUndelivered";
    String subscription = "subscription";
    MessageMetadata firstDiscarded = TestMessageMetadata.of("1234", topic, subscription);
    long firstTimestamp = System.currentTimeMillis();

    MessageMetadata secondDiscarded = TestMessageMetadata.of("5678", topic, subscription);
    // difference between first and second timestamp must be bigger than 1000ms as query sorts by
    // seconds
    long secondTimestamp = firstTimestamp + 2000;

    // when
    consumersLogRepository.logDiscarded(firstDiscarded, firstTimestamp, REASON_MESSAGE);
    consumersLogRepository.logDiscarded(secondDiscarded, secondTimestamp, REASON_MESSAGE);

    // then
    assertThat(fetchLastUndelivered(topic, subscription))
        .containsExactly(sentMessageTrace(secondDiscarded, secondTimestamp, DISCARDED));
  }

  @Test
  public void shouldGetMessageStatus() {
    // given
    String datacenter = "dc1";
    MessageMetadata messageMetadata =
        TestMessageMetadata.of("1234", "elasticsearch.messageStatus", "subscription");
    long timestamp = System.currentTimeMillis();
    ImmutableMap<String, String> extraRequestHeaders =
        ImmutableMap.of("x-header1", "value1", "x-header2", "value2");

    frontendLogRepository.logPublished(
        "1234",
        timestamp,
        "elasticsearch.messageStatus",
        "localhost",
        datacenter,
        extraRequestHeaders);
    consumersLogRepository.logSuccessful(messageMetadata, "localhost", timestamp);

    // when
    assertThat(fetchMessageStatus(messageMetadata))
        .contains(
            publishedMessageTrace(
                messageMetadata,
                extraRequestHeaders,
                timestamp,
                PublishedMessageTraceStatus.SUCCESS,
                datacenter))
        .contains(sentMessageTrace(messageMetadata, timestamp, SentMessageTraceStatus.SUCCESS));
  }

  private List<SentMessageTrace> fetchLastUndelivered(String topic, String subscription) {
    final List<SentMessageTrace> lastUndelivered = new ArrayList<>();

    await()
        .atMost(Duration.ofMinutes(1))
        .until(
            () -> {
              lastUndelivered.clear();
              lastUndelivered.addAll(
                  logRepository.getLastUndeliveredMessages(topic, subscription, 1));
              return lastUndelivered.size() == 1;
            });
    return lastUndelivered;
  }

  private List<MessageTrace> fetchMessageStatus(MessageMetadata messageMetadata) {
    List<MessageTrace> status = new ArrayList<>();

    await()
        .atMost(Duration.ofMinutes(1))
        .until(
            () -> {
              status.clear();
              status.addAll(
                  logRepository.getMessageStatus(
                      messageMetadata.getTopic(),
                      messageMetadata.getSubscription(),
                      messageMetadata.getMessageId()));
              return status.size() == 2;
            });

    return status;
  }

  private SentMessageTrace sentMessageTrace(
      MessageMetadata messageMetadata, long timestamp, SentMessageTraceStatus status) {
    return SentMessageTrace.Builder.sentMessageTrace(
            messageMetadata.getMessageId(), messageMetadata.getBatchId(), status)
        .withTimestamp(timestamp)
        .withSubscription(messageMetadata.getSubscription())
        .withTopicName(messageMetadata.getTopic())
        .withReason(REASON_MESSAGE)
        .withPartition(messageMetadata.getPartition())
        .withOffset(messageMetadata.getOffset())
        .withCluster(CLUSTER_NAME)
        .build();
  }

  private PublishedMessageTrace publishedMessageTrace(
      MessageMetadata messageMetadata,
      Map<String, String> extraRequestHeaders,
      long timestamp,
      PublishedMessageTraceStatus status,
      String storageDatacenter) {
    return new PublishedMessageTrace(
        messageMetadata.getMessageId(),
        timestamp,
        messageMetadata.getTopic(),
        status,
        null,
        null,
        CLUSTER_NAME,
        extraRequestHeaders.entrySet().stream().collect(extraRequestHeadersCollector()),
        storageDatacenter);
  }
}
