package pl.allegro.tech.hermes.client.metrics;

import com.codahale.metrics.MetricRegistry;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DropwizardMetricsProvider implements MetricsProvider {

    private final MetricRegistry metrics;


    public DropwizardMetricsProvider(MetricRegistry metrics) {
        this.metrics = metrics;
    }

    @Override
    public void counterIncrement(String topic, String key) {
        metrics.counter(prefix + topic + "." + key).inc();
    }

    @Override
    public void counterIncrement(String topic, String key, Map<String, String> tags) {
        counterIncrement(topic, buildCounterName(key, tags));
    }

    @Override
    public void timerRecord(String topic, String key, long duration, TimeUnit unit) {
        metrics.timer(prefix + topic + "." + key).update(duration, unit);
    }

    @Override
    public void histogramUpdate(String topic, String key, int value) {
        metrics.histogram(prefix + topic + "." + key).update(value);
    }

    private String buildCounterName(String key, Map<String, String> tags) {
        return key + "." + String.join(".", tags.values());
    }
}
