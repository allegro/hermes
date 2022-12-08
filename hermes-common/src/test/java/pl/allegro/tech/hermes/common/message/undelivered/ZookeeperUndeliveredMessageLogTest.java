package pl.allegro.tech.hermes.common.message.undelivered;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.SentMessageTrace.Builder.undeliveredMessage;
import static pl.allegro.tech.hermes.common.metric.Histograms.PERSISTED_UNDELIVERED_MESSAGE_SIZE;
import static pl.allegro.tech.hermes.common.metric.Meters.PERSISTED_UNDELIVERED_MESSAGES_METER;

public class ZookeeperUndeliveredMessageLogTest extends ZookeeperBaseTest {

    private static final TopicName TOPIC = new TopicName("undeliveredMessageLogGroup", "topic");

    private static final String SUBSCRIPTION = "subscription";

    private final ZookeeperPaths paths = new ZookeeperPaths("/hermes");

    private final HermesMetrics hermesMetrics = new HermesMetrics(new MetricRegistry(), new PathsCompiler("host"));

    private final ZookeeperUndeliveredMessageLog log = new ZookeeperUndeliveredMessageLog(
            zookeeperClient,
            paths,
            new ObjectMapper(),
            hermesMetrics
    );

    private final ZookeeperLastUndeliveredMessageReader reader = new ZookeeperLastUndeliveredMessageReader(
            zookeeperClient,
            paths,
            new ObjectMapper()
    );

    @Before
    public void setUp() throws Exception {
        zookeeperClient.create().creatingParentsIfNeeded().forPath(paths.subscriptionPath(TOPIC, SUBSCRIPTION));
    }

    @After
    public void cleanUp() throws Exception {
        deleteData(paths.basePath());
        hermesMetrics.unregister(PERSISTED_UNDELIVERED_MESSAGES_METER);
        hermesMetrics.unregister(PERSISTED_UNDELIVERED_MESSAGE_SIZE);
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
        Optional<SentMessageTrace> result = reader.last(new TopicName("unknown", "topic"), "subscription");

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
        assertThat(hermesMetrics.meter(PERSISTED_UNDELIVERED_MESSAGES_METER).getCount()).isEqualTo(persistedMessageCount);
        assertThat(hermesMetrics.histogram(PERSISTED_UNDELIVERED_MESSAGE_SIZE).getCount()).isEqualTo(persistedMessageCount);
    }
}
