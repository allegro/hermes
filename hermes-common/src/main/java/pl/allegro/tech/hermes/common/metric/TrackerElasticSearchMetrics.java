package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import pl.allegro.tech.hermes.metrics.HermesTimer;

import java.util.concurrent.BlockingQueue;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.HOSTNAME;

public class TrackerElasticSearchMetrics {
    private final MeterRegistry meterRegistry;
    private final HermesMetrics hermesMetrics;

    public TrackerElasticSearchMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.hermesMetrics = hermesMetrics;
    }

    public void registerProducerTrackerElasticSearchQueueSizeGauge(BlockingQueue<?> queue) {
        registerQueueSizeGauge(Gauges.Graphite.PRODUCER_TRACKER_ELASTICSEARCH_QUEUE_SIZE,
                Gauges.Prometheus.TRACKER_ELASTICSEARCH_QUEUE_SIZE, queue);
    }

    public void registerProducerTrackerElasticSearchRemainingCapacity(BlockingQueue<?> queue) {
        registerRemainingCapacityGauge(Gauges.Graphite.PRODUCER_TRACKER_ELASTICSEARCH_REMAINING_CAPACITY,
                Gauges.Prometheus.TRACKER_ELASTICSEARCH_REMAINING_CAPACITY, queue);
    }

    public void registerConsumerTrackerElasticSearchQueueSizeGauge(BlockingQueue<?> queue) {
        registerQueueSizeGauge(Gauges.Graphite.CONSUMER_TRACKER_ELASTICSEARCH_QUEUE_SIZE,
                Gauges.Prometheus.TRACKER_ELASTICSEARCH_QUEUE_SIZE, queue);
    }

    public void registerConsumerTrackerElasticSearchRemainingCapacity(BlockingQueue<?> queue) {
        registerRemainingCapacityGauge(Gauges.Graphite.CONSUMER_TRACKER_ELASTICSEARCH_REMAINING_CAPACITY,
                Gauges.Prometheus.TRACKER_ELASTICSEARCH_REMAINING_CAPACITY, queue);
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
        private static class Graphite {
            public static final String PRODUCER_TRACKER_ELASTICSEARCH_QUEUE_SIZE =
                    "producer." + HOSTNAME + ".tracker.elasticsearch.queue-size";
            public static final String PRODUCER_TRACKER_ELASTICSEARCH_REMAINING_CAPACITY =
                    "producer." + HOSTNAME + ".tracker.elasticsearch.remaining-capacity";

            public static final String CONSUMER_TRACKER_ELASTICSEARCH_QUEUE_SIZE =
                    "consumer." + HOSTNAME + ".tracker.elasticsearch.queue-size";
            public static final String CONSUMER_TRACKER_ELASTICSEARCH_REMAINING_CAPACITY =
                    "consumer." + HOSTNAME + ".tracker.elasticsearch.remaining-capacity";
        }

        private static class Prometheus {
            public static final String TRACKER_ELASTICSEARCH_QUEUE_SIZE = "tracker.elasticsearch.queue-size";
            public static final String TRACKER_ELASTICSEARCH_REMAINING_CAPACITY = "tracker.elasticsearch.remaining-capacity";
        }
    }

    private static class Timers {
        public static final String ELASTICSEARCH_COMMIT_LATENCY = "tracker.elasticsearch.commit-latency";
    }
}
