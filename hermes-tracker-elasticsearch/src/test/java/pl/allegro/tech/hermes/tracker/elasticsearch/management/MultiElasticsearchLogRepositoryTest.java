package pl.allegro.tech.hermes.tracker.elasticsearch.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.common.http.ExtraRequestHeadersCollector.extraRequestHeadersCollector;

import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersDailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersElasticsearchLogRepository;
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendDailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendElasticsearchLogRepository;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendIndexFactory;
import pl.allegro.tech.hermes.tracker.management.LogRepository;

public class MultiElasticsearchLogRepositoryTest implements LogSchemaAware {

  private static final String CLUSTER_NAME = "primary";
  private static final String REASON_MESSAGE = "Bad Request";

  private static final Clock clock =
      Clock.fixed(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.systemDefault());
  private static final FrontendIndexFactory frontendIndexFactory =
      new FrontendDailyIndexFactory(clock);
  private static final ConsumersIndexFactory consumersIndexFactory =
      new ConsumersDailyIndexFactory(clock);
  private static final MetricsFacade metricsFacade = new MetricsFacade(new SimpleMeterRegistry());

  private static final ElasticsearchResource elasticsearch1 = new ElasticsearchResource();
  private static final ElasticsearchResource elasticsearch2 = new ElasticsearchResource();

  private LogRepository logRepository;
  private FrontendElasticsearchLogRepository frontendLogRepository;
  private ConsumersElasticsearchLogRepository consumersLogRepository;

  @BeforeClass
  public static void beforeAll() throws Throwable {
    elasticsearch1.before();
    elasticsearch2.before();
  }

  @AfterClass
  public static void afterAll() {
    elasticsearch1.after();
    elasticsearch2.after();
  }

  @Before
  public void setUp() {
    logRepository =
        new MultiElasticsearchLogRepository(
            Arrays.asList(elasticsearch1.client(), elasticsearch2.client()));

    frontendLogRepository =
        new FrontendElasticsearchLogRepository.Builder(elasticsearch1.client(), metricsFacade)
            .withIndexFactory(frontendIndexFactory)
            .build();

    consumersLogRepository =
        new ConsumersElasticsearchLogRepository.Builder(elasticsearch2.client(), metricsFacade)
            .withIndexFactory(consumersIndexFactory)
            .build();
  }

  @Test
  public void shouldGetMessageStatus() {
    // given
    String datacenter = "dc1";
    Map<String, String> extraRequestHeaders =
        ImmutableMap.of("x-header1", "value1", "x-header2", "value2");
    MessageMetadata messageMetadata =
        TestMessageMetadata.of("1234", "elasticsearch1.messageStatus", "subscription");
    long timestamp = System.currentTimeMillis();

    // when
    frontendLogRepository.logPublished(
        "1234",
        timestamp,
        "elasticsearch1.messageStatus",
        "localhost",
        datacenter,
        extraRequestHeaders);
    consumersLogRepository.logSuccessful(messageMetadata, "localhost", timestamp);

    // then
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
