package pl.allegro.tech.hermes.client.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.TimeUnit;

public class MicrometerMetricsProvider implements MetricsProvider {

    private final MeterRegistry metrics;

    public MicrometerMetricsProvider(MeterRegistry metrics) {
        this.metrics = metrics;
    }

    @Override
    public void counterIncrement(String name) {
        metrics.counter(name).increment();
    }

    @Override
    public void timerRecord(String name, long duration, TimeUnit unit) {
        metrics.timer(name).record(duration, unit);
    }

    @Override
    public void histogramUpdate(String name, int value) {
        DistributionSummary.builder(name)
                .publishPercentiles(0, 0.5, 0.9, 0.95, 0.99)
                .register(metrics)
                .record(value);
    }
}
