package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import pl.allegro.tech.hermes.metrics.HermesTimer;

import java.util.function.ToDoubleFunction;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.HOSTNAME;

public class TrackerElasticSearchMetrics {
    private final MeterRegistry meterRegistry;
    private final HermesMetrics hermesMetrics;
    private final GaugeRegistrar gaugeRegistrar;

    public TrackerElasticSearchMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.hermesMetrics = hermesMetrics;
        this.gaugeRegistrar = new GaugeRegistrar(meterRegistry, hermesMetrics);
    }

    public <T> void registerProducerTrackerElasticSearchQueueSizeGauge(T stateObj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(
                Gauges.Prometheus.TRACKER_ELASTICSEARCH_QUEUE_SIZE,
                stateObj, f
        );
    }

    public <T> void registerProducerTrackerElasticSearchRemainingCapacity(T stateObj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(
                Gauges.Prometheus.TRACKER_ELASTICSEARCH_REMAINING_CAPACITY,
                stateObj, f
        );
    }

    public <T> void registerConsumerTrackerElasticSearchQueueSizeGauge(T stateObj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(
                Gauges.Prometheus.TRACKER_ELASTICSEARCH_QUEUE_SIZE,
                stateObj, f
        );
    }

    public <T> void registerConsumerTrackerElasticSearchRemainingCapacity(T stateObj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(
                Gauges.Prometheus.TRACKER_ELASTICSEARCH_REMAINING_CAPACITY,
                stateObj, f
        );
    }

    public HermesTimer trackerElasticSearchCommitLatencyTimer() {
        return HermesTimer.from(
                meterRegistry.timer(Timers.ELASTICSEARCH_COMMIT_LATENCY),
                hermesMetrics.timer(Timers.ELASTICSEARCH_COMMIT_LATENCY)
        );
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
