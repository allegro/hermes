package pl.allegro.tech.hermes.consumers.consumer.offset.kafka;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import java.util.concurrent.ExecutorService;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class KafkaAsyncOffsetMonitorTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HermesMetrics hermesMetrics;

    @Mock
    private KafkaLatestOffsetReader latestOffsetReader;

    @Mock
    private ExecutorService executorService;

    private KafkaAsyncOffsetMonitor offsetMonitor;

    @Before
    public void setUp() {
        offsetMonitor = new KafkaAsyncOffsetMonitor(latestOffsetReader, hermesMetrics);
        offsetMonitor.setExecutorService(executorService);
    }

    @Test
    public void shouldMeterLagAsynchronously() {

        offsetMonitor.process(Maps.<Subscription, PartitionOffset>newHashMap());

        verify(executorService).execute(any(KafkaOffsetMonitor.class));
    }

}