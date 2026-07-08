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

  public void recordBrokerLatency(
      String broker, String brokerDc, Topic.Ack ack, Duration duration) {
    Timer.builder("broker.latency")
        .tag("broker", broker)
        .tag("broker_dc", brokerDc)
        .tag("ack", ack.name())
        .register(meterRegistry)
        .record(duration);
  }
}
