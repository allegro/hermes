package pl.allegro.tech.hermes.client.metrics;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface MetricsProvider {

  String prefix = "hermes-client.";

  void counterIncrement(String topic, String key);

  void counterIncrement(String topic, String key, Map<String, String> tags);

  void timerRecord(String topic, String key, long duration, TimeUnit unit);

  void histogramUpdate(String topic, String key, int value);
}
