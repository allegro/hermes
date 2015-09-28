package pl.allegro.tech.hermes.consumers.consumer.offset;

import com.codahale.metrics.Counter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.time.Clock;

import static org.mockito.Mockito.*;

public class OffsetCommitQueueMonitorTest {

    private Subscription subscription = Subscription.Builder.subscription()
            .applyDefaults().withSubscriptionPolicy(
                    SubscriptionPolicy.Builder.subscriptionPolicy()
                            .applyDefaults().withRate(1000).withMessageTtl(5).build()
            )
            .withTopicName("group", "topic")
            .withName("subscription")
            .build();

    private HermesMetrics hermesMetrics = Mockito.mock(HermesMetrics.class);

    private Clock clock = Mockito.mock(Clock.class);

    private OffsetCommitQueueMonitor monitor;

    private final TopicPartition topicPartition = new TopicPartition(new KafkaTopic("topic"), 5);

    @Before
    public void setUp() {

        int offsetCommitAdditionalIdlePeriodAlert = 1000;
        int offsetCommitQueueAlertSize = 100;

        monitor = new OffsetCommitQueueMonitor(subscription, topicPartition, hermesMetrics, clock, offsetCommitAdditionalIdlePeriodAlert, offsetCommitQueueAlertSize);

        Counter idlenessCounterMock = Mockito.mock(Counter.class);

        when(hermesMetrics.counterForOffsetCommitIdlePeriod(subscription, topicPartition.getTopic(), topicPartition.getPartition())).thenReturn(idlenessCounterMock);

    }

    @Test
    public void shouldReportIdlenessWhenSizeIsTooLarge() {
        // when
        monitor.newOffsetCommit();

        monitor.nothingNewToCommit(1000, 5L);
        monitor.nothingNewToCommit(1000, 5L);

        // then
        verify(hermesMetrics).removeCounterForOffsetCommitIdlePeriod(subscription, topicPartition.getTopic(), topicPartition.getPartition());
        verify(hermesMetrics).counterForOffsetCommitIdlePeriod(subscription, topicPartition.getTopic(), topicPartition.getPartition());
    }

    @Test
    public void shouldNotReportIdlenessWhenSizeIsSmall() {
        // when
        monitor.newOffsetCommit();

        monitor.nothingNewToCommit(10, 5L);
        monitor.nothingNewToCommit(10, 5L);

        // then
        verify(hermesMetrics).removeCounterForOffsetCommitIdlePeriod(subscription, topicPartition.getTopic(), topicPartition.getPartition());
        Mockito.verifyNoMoreInteractions(hermesMetrics);
    }

    @Test
    public void shouldReportIdlenessWhenIdlePeriodIsTooLong() {
        // given
        when(clock.getTime()).thenReturn(1000L).thenReturn(2_000_000L);

        // when
        monitor.newOffsetCommit();

        monitor.nothingNewToCommit(10, 5L);
        monitor.nothingNewToCommit(10, 5L);

        // then
        verify(hermesMetrics).removeCounterForOffsetCommitIdlePeriod(subscription, topicPartition.getTopic(), topicPartition.getPartition());
        verify(hermesMetrics).counterForOffsetCommitIdlePeriod(subscription, topicPartition.getTopic(), topicPartition.getPartition());
    }


    @Test
    public void shouldNotReportIdlenessWhenIdlePeriodIsTooLongButOffsetHasChanged() {
        // given
        when(clock.getTime()).thenReturn(1000L).thenReturn(1_000_000_000L);

        // when
        monitor.newOffsetCommit();

        monitor.nothingNewToCommit(10, 5L);
        monitor.nothingNewToCommit(10, 7L);

        // then
        verify(hermesMetrics).removeCounterForOffsetCommitIdlePeriod(subscription, topicPartition.getTopic(), topicPartition.getPartition());
        verifyZeroInteractions(hermesMetrics);
    }

    @Test
    public void shouldNotReportIdlenessWhenNothingToCommitMethodIsCalledOnce() {
        // given
        when(clock.getTime()).thenReturn(1000L).thenReturn(1_000_000_000L);

        // when
        monitor.newOffsetCommit();

        monitor.nothingNewToCommit(10, 5L);

        // then
        verify(hermesMetrics).removeCounterForOffsetCommitIdlePeriod(subscription, topicPartition.getTopic(), topicPartition.getPartition());
        verifyZeroInteractions(hermesMetrics);
    }
}
