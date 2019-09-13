package pl.allegro.tech.hermes.client.metrics;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface MetricsProvider {

    void counterIncrement(String name);

    void counterIncrement(String prefix, String name, Map<String, String> tags);

    void timerRecord(String name, long duration, TimeUnit unit);

    void histogramUpdate(String name, int value);
}
