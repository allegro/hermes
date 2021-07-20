package pl.allegro.tech.hermes.client.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MicrometerMetricsProvider implements MetricsProvider {

    private final MeterRegistry metrics;

    public MicrometerMetricsProvider(MeterRegistry metrics) {
        this.metrics = metrics;
    }

    @Override
    public void counterIncrement(String topic, String key) {
        counterIncrement(topic, key, new HashMap<>());
    }

    @Override
    public void counterIncrement(String topic, String key, Map<String, String> tags) {
        metrics.counter(buildCounterName(topic, key, tags), Tags.of(tags.entrySet().stream()
               .map(e -> Tag.of(e.getKey(), e.getValue()))
               .collect(Collectors.toSet())))
               .increment();
    }

    @Override
    public void timerRecord(String topic, String key, long duration, TimeUnit unit) {
        Map<String, String> tags = new HashMap<>();
        metrics.timer(buildCounterName(topic, key, tags), Tags.of(tags.entrySet().stream()
                .map(e -> Tag.of(e.getKey(), e.getValue()))
                .collect(Collectors.toSet())))
                .record(duration, unit);
    }

    @Override
    public void histogramUpdate(String topic, String key, int value) {
        Map<String, String> tags = new HashMap<>();
        metrics.summary(buildCounterName(topic, key, tags), Tags.of(tags.entrySet().stream()
                .map(e -> Tag.of(e.getKey(), e.getValue()))
                .collect(Collectors.toSet())))
                .record(value);
    }

    private String buildCounterName(String topic, String key, Map<String, String> tags) {
        tags.put("topic", topic);
        tags.put("key", key);
        return prefix + "{topic}.{key}"
                + (tags.size() > 2 ? "." : "")
                + tags.keySet().stream().filter(e -> !e.equals("topic") && !e.equals("key")).map(e -> "{" + e + "}").collect(Collectors.joining("."));
    }
}
