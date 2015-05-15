package pl.allegro.tech.hermes.common.message.tracker;

import com.codahale.metrics.Gauge;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Metrics;

import java.util.concurrent.BlockingQueue;

public final class QueueMetrics {

    private QueueMetrics() {
    }

    public static void registerCurrentSizeGauge(final BlockingQueue queue,
                                                final Metrics.Gauge gauge,
                                                final HermesMetrics metrics) {
        metrics.registerGauge(gauge, new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return queue.size();
            }
        });
    }

    public static void registerRemainingCapacityGauge(final BlockingQueue queue,
                                                      final Metrics.Gauge gauge,
                                                      final HermesMetrics metrics) {
        metrics.registerGauge(gauge, new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return queue.remainingCapacity();
            }
        });
    }
}
