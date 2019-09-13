package pl.allegro.tech.hermes.client.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    public void counterIncrement(String prefix, String name, Map<String, String> tags) {
        metrics.counter(buildCounterName(prefix, name, tags), Tags.of(tags.entrySet().stream()
               .map(e -> Tag.of(e.getKey(), e.getValue()))
               .collect(Collectors.toSet())))
               .increment();
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

    private String buildCounterName(String prefix, String name, Map<String, String> tags) {
        return prefix + "." + name + "."
                + tags.keySet().stream().map(e -> "{" + e + "}").collect(Collectors.joining("."));
    }
}
