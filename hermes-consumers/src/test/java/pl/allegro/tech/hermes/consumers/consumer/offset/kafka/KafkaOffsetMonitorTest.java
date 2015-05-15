package pl.allegro.tech.hermes.consumers.consumer.offset.kafka;

import com.google.common.collect.ImmutableMap;
import kafka.common.TopicAndPartition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.counter.MetricsDeltaCalculator;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;

@RunWith(MockitoJUnitRunner.class)
public class KafkaOffsetMonitorTest {

    private static final int PARTITION_NUMBER = 8;
    private static final Subscription SUBSCRIPTION_1 = subscription()
            .withTopicName("pl.allegro.tech.hermes.topic1")
            .withName("pl.allegro.tech.hermes.subscription1")
            .build();
    private static final Subscription SUBSCRIPTION_2 = subscription()
            .withTopicName("pl.allegro.tech.hermes.topic2")
            .withName("pl.allegro.tech.hermes.subscription2")
            .build();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HermesMetrics hermesMetrics;

    @Mock
    private ConfigFactory configFactory;

    @Mock
    private KafkaLatestOffsetReader latestOffsetReader;

    @Mock
    private MetricsDeltaCalculator metricsDeltaCalculator;

    @Before
    public void setUp() {
        when(metricsDeltaCalculator.calculateDelta(any(String.class), any(Long.class))).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                return (Long) invocation.getArguments()[1];
            }
        });
    }

    @Test
    public void shouldMeterLags() {
        //given
        final long offset1 = 10;
        final long offset2 = 30;
        final long latestOffset1 = 50;
        final long latestOffset2 = 60;
        when(latestOffsetReader.read(anySetOf(TopicAndPartition.class))).thenReturn(ImmutableMap.of(
                new TopicAndPartition(SUBSCRIPTION_1.getQualifiedTopicName(), PARTITION_NUMBER), latestOffset1,
                new TopicAndPartition(SUBSCRIPTION_2.getQualifiedTopicName(), PARTITION_NUMBER), latestOffset2
        ));
        Map<Subscription, PartitionOffset> offsetsPerSubscription = ImmutableMap.of(
                SUBSCRIPTION_1, new PartitionOffset(offset1, PARTITION_NUMBER),
                SUBSCRIPTION_2, new PartitionOffset(offset2, PARTITION_NUMBER)
        );
        KafkaOffsetMonitor offsetMonitor = new KafkaOffsetMonitor(
            offsetsPerSubscription, latestOffsetReader, hermesMetrics, metricsDeltaCalculator
        );

        //when
        offsetMonitor.run();

        //then
        verify(hermesMetrics.counterForOffsetLag(SUBSCRIPTION_1, PARTITION_NUMBER)).inc(latestOffset1 - offset1);
        verify(hermesMetrics.counterForOffsetLag(SUBSCRIPTION_2, PARTITION_NUMBER)).inc(latestOffset2 - offset2);
    }

    @Test
    public void shouldNotThrowException() {
        //given
        KafkaOffsetMonitor offsetMonitor = new KafkaOffsetMonitor(
                ImmutableMap.of(SUBSCRIPTION_1, new PartitionOffset(1, PARTITION_NUMBER)),
                latestOffsetReader,
                hermesMetrics,
                metricsDeltaCalculator
        );

        //when
        offsetMonitor.run();

        //then
        verify(hermesMetrics, never()).counterForOffsetLag(any(Subscription.class), any(Integer.class));
    }

}