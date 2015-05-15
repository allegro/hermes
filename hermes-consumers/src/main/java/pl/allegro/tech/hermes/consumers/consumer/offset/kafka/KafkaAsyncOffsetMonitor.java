package pl.allegro.tech.hermes.consumers.consumer.offset.kafka;

import com.google.common.annotations.VisibleForTesting;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.counter.MetricsDeltaCalculator;
import pl.allegro.tech.hermes.consumers.consumer.offset.AsyncOffsetMonitor;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KafkaAsyncOffsetMonitor implements AsyncOffsetMonitor {

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private KafkaLatestOffsetReader kafkaLatestOffsetReader;
    private HermesMetrics hermesMetrics;
    private MetricsDeltaCalculator metricsDeltaCalculator = new MetricsDeltaCalculator();

    @Inject
    public KafkaAsyncOffsetMonitor(KafkaLatestOffsetReader kafkaLatestOffsetReader, HermesMetrics hermesMetrics) {
        this.kafkaLatestOffsetReader = kafkaLatestOffsetReader;
        this.hermesMetrics = hermesMetrics;
    }

    @Override
    public void process(Map<Subscription, PartitionOffset> offsetsPerSubscription) {
        executorService.execute(
            new KafkaOffsetMonitor(offsetsPerSubscription, kafkaLatestOffsetReader, hermesMetrics, metricsDeltaCalculator
        ));
    }

    @VisibleForTesting
    protected void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

}
