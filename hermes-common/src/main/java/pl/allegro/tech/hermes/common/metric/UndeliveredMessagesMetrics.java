package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import pl.allegro.tech.hermes.metrics.DefaultHermesHistogram;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesHistogram;
import pl.allegro.tech.hermes.metrics.counters.HermesCounters;

public class UndeliveredMessagesMetrics {
  private final MeterRegistry meterRegistry;

  public UndeliveredMessagesMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public HermesCounter undeliveredMessagesCounter() {
    return HermesCounters.from(meterRegistry.counter("undelivered-messages.persisted"));
  }

  public HermesHistogram undeliveredMessagesSizeHistogram() {
    return DefaultHermesHistogram.of(
        DistributionSummary.builder("undelivered-messages.persisted.message-size.bytes")
            .register(meterRegistry));
  }
}
