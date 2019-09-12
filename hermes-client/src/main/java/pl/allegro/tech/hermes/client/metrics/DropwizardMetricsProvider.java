package pl.allegro.tech.hermes.client.metrics;

import com.codahale.metrics.MetricRegistry;

import java.util.concurrent.TimeUnit;

public class DropwizardMetricsProvider implements MetricsProvider {

    private final MetricRegistry metrics;

    public DropwizardMetricsProvider(MetricRegistry metrics) {
        this.metrics = metrics;
    }

    @Override
    public void counterIncrement(String name) {
        metrics.counter(name).inc();
    }

    @Override
    public void timerRecord(String name, long duration, TimeUnit unit) {
        metrics.timer(name).update(duration, unit);
    }

    @Override
    public void histogramUpdate(String name, int value) {
        metrics.histogram(name).update(value);
    }
}
