package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import pl.allegro.tech.hermes.api.Topic;

public class BrokerMetrics {
  private final MeterRegistry meterRegistry;

  public BrokerMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void recordBrokerLatency(String broker, Topic.Ack ack, Duration duration) {
    Timer.builder("broker.latency")
        .tag("broker", broker)
        .tag("ack", ack.name())
        .publishPercentileHistogram()
        .maximumExpectedValue(Duration.ofSeconds(5))
        .register(meterRegistry)
        .record(duration);
  }
}
