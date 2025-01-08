package pl.allegro.tech.hermes.common.message.undelivered;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.SentMessageTrace.Builder.undeliveredMessage;
import static pl.allegro.tech.hermes.common.metric.Histograms.PERSISTED_UNDELIVERED_MESSAGE_SIZE;
import static pl.allegro.tech.hermes.common.metric.Meters.PERSISTED_UNDELIVERED_MESSAGES_METER;
import static pl.allegro.tech.hermes.test.helper.metrics.MicrometerUtils.metricValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

public class ZookeeperUndeliveredMessageLogTest extends ZookeeperBaseTest {

  private static final TopicName TOPIC = new TopicName("undeliveredMessageLogGroup", "topic");

  private static final String SUBSCRIPTION = "subscription";

  private final ZookeeperPaths paths = new ZookeeperPaths("/hermes");

  private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final MetricsFacade metricsFacade = new MetricsFacade(meterRegistry);

  private final ZookeeperUndeliveredMessageLog log =
      new ZookeeperUndeliveredMessageLog(zookeeperClient, paths, new ObjectMapper(), metricsFacade);

  private final ZookeeperLastUndeliveredMessageReader reader =
      new ZookeeperLastUndeliveredMessageReader(zookeeperClient, paths, new ObjectMapper());

  @Before
  public void setUp() throws Exception {
    zookeeperClient
        .create()
        .creatingParentsIfNeeded()
        .forPath(paths.subscriptionPath(TOPIC, SUBSCRIPTION));
  }

  @After
  public void cleanUp() throws Exception {
    deleteData(paths.basePath());
  }

  @Test
  public void shouldAddUndeliveredMessageToLog() throws Exception {
    // given when
    log.add(createUndeliveredMessage(SUBSCRIPTION, "message"));
    log.persist();

    // then
    SentMessageTrace lastMessage = reader.last(TOPIC, "subscription").get();
    assertThat(lastMessage.getMessage()).isEqualTo("message");
    assertThatMetricsHaveBeenReported(1);
  }

  @Test
  public void shouldReturnLatestUndeliveredMessage() throws Exception {
    // given
    log.add(createUndeliveredMessage(SUBSCRIPTION, "old message"));
    log.add(createUndeliveredMessage(SUBSCRIPTION, "new message"));
    log.persist();

    // when
    SentMessageTrace lastMessage = reader.last(TOPIC, "subscription").get();

    // then
    assertThat(lastMessage.getMessage()).isEqualTo("new message");
    assertThatMetricsHaveBeenReported(1);
  }

  @Test
  public void shouldReturnAbsentIfThereAreNoUndeliveredMessagesForGivenSubscription() {
    // when
    Optional<SentMessageTrace> result =
        reader.last(new TopicName("unknown", "topic"), "subscription");

    // then
    assertThat(result.isPresent()).isFalse();
    assertThatMetricsHaveBeenReported(0);
  }

  @Test
  public void shouldNotAddUndeliveredMessageLogToNonExistingSubscriptionPath() {
    // given
    log.add(createUndeliveredMessage("unknownSubscription", "message"));
    log.persist();

    // when
    Optional<SentMessageTrace> result = reader.last(TOPIC, "unknownSubscription");

    // then
    assertThat(result.isPresent()).isFalse();
    assertThatMetricsHaveBeenReported(0);
  }

  private SentMessageTrace createUndeliveredMessage(String subscription, String message) {
    return undeliveredMessage()
        .withTopicName(TOPIC.qualifiedName())
        .withSubscription(subscription)
        .withMessage(message)
        .withReason(new IllegalArgumentException().getMessage())
        .withTimestamp(1L)
        .withPartition(1)
        .withOffset(1L)
        .withCluster("cluster")
        .build();
  }

  private void assertThatMetricsHaveBeenReported(int persistedMessageCount) {
    assertThat(
            metricValue(
                    meterRegistry,
                    PERSISTED_UNDELIVERED_MESSAGES_METER,
                    Search::counter,
                    Counter::count)
                .orElse(0.0d))
        .isEqualTo(persistedMessageCount);
    assertThat(
            metricValue(
                    meterRegistry,
                    PERSISTED_UNDELIVERED_MESSAGE_SIZE + ".bytes",
                    Search::summary,
                    DistributionSummary::count)
                .orElse(0L))
        .isEqualTo(persistedMessageCount);
  }
}
