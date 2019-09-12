package pl.allegro.tech.hermes.client.metrics;

import java.util.concurrent.TimeUnit;

public interface MetricsProvider {

    void counterIncrement(String name);

    void timerRecord(String name, long duration, TimeUnit unit);

    void histogramUpdate(String name, int value);
}
