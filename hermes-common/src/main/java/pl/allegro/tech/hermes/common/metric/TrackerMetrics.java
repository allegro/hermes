package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import pl.allegro.tech.hermes.metrics.HermesTimer;

import java.util.concurrent.BlockingQueue;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.HOSTNAME;

public class TrackerMetrics {
    private final MeterRegistry meterRegistry;
    private final HermesMetrics hermesMetrics;

    public TrackerMetrics(MeterRegistry meterRegistry, HermesMetrics hermesMetrics) {
        this.meterRegistry = meterRegistry;
        this.hermesMetrics = hermesMetrics;
    }

    public void producerTrackerElasticSearchQueueSizeGauge(BlockingQueue<?> queue) {
        registerQueueSizeGauge(Gauges.PRODUCER_TRACKER_ELASTICSEARCH_QUEUE_SIZE, "tracker.elasticsearch.queue-size", queue);
    }

    public void producerTrackerElasticSearchRemainingCapacity(BlockingQueue<?> queue) {
        registerRemainingCapacityGauge(Gauges.PRODUCER_TRACKER_ELASTICSEARCH_REMAINING_CAPACITY, "tracker.elasticsearch.remaining-capacity", queue);
    }

    public void consumerTrackerElasticSearchQueueSizeGauge(BlockingQueue<?> queue) {
        registerQueueSizeGauge(Gauges.CONSUMER_TRACKER_ELASTICSEARCH_QUEUE_SIZE, "tracker.elasticsearch.queue-size", queue);
    }

    public void consumerTrackerElasticSearchRemainingCapacity(BlockingQueue<?> queue) {
        registerRemainingCapacityGauge(Gauges.CONSUMER_TRACKER_ELASTICSEARCH_REMAINING_CAPACITY, "tracker.elasticsearch.remaining-capacity", queue);
    }

    public HermesTimer trackerElasticSearchCommitLatencyTimer() {
        return HermesTimer.from(
                meterRegistry.timer(Timers.ELASTICSEARCH_COMMIT_LATENCY),
                hermesMetrics.timer(Timers.ELASTICSEARCH_COMMIT_LATENCY)
        );
    }

    private void registerQueueSizeGauge(String graphiteMetricName, String prometheusMetricName, BlockingQueue<?> queue) {
        this.hermesMetrics.registerGauge(graphiteMetricName, queue::size);
        this.meterRegistry.gaugeCollectionSize(prometheusMetricName, Tags.empty(), queue);
    }

    private void registerRemainingCapacityGauge(String graphiteMetricName, String prometheusMetricName, BlockingQueue<?> queue) {
        this.hermesMetrics.registerGauge(graphiteMetricName, queue::remainingCapacity);
        this.meterRegistry.gauge(prometheusMetricName, Tags.empty(), queue, BlockingQueue::remainingCapacity);
    }

    private static class Gauges {
            public static final String PRODUCER_TRACKER_ELASTICSEARCH_QUEUE_SIZE =
                    "producer." + HOSTNAME + ".tracker.elasticsearch.queue-size";
            public static final String PRODUCER_TRACKER_ELASTICSEARCH_REMAINING_CAPACITY =
                    "producer." + HOSTNAME + ".tracker.elasticsearch.remaining-capacity";

            public static final String CONSUMER_TRACKER_ELASTICSEARCH_QUEUE_SIZE = "consumer." + HOSTNAME + ".tracker.elasticsearch.queue-size";
            public static final String CONSUMER_TRACKER_ELASTICSEARCH_REMAINING_CAPACITY =
                    "consumer." + HOSTNAME + ".tracker.elasticsearch.remaining-capacity";
    }

    private static class Timers {
        public static final String ELASTICSEARCH_COMMIT_LATENCY = "tracker.elasticsearch.commit-latency";
    }
}
