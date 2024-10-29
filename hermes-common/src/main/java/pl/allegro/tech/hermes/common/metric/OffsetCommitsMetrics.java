package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.counters.HermesCounters;

public class OffsetCommitsMetrics {

  private final MeterRegistry meterRegistry;

  OffsetCommitsMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public HermesCounter skippedCounter() {
    return HermesCounters.from(meterRegistry.counter("offset-commits.skipped"));
  }

  public HermesCounter obsoleteCounter() {
    return HermesCounters.from(meterRegistry.counter("offset-commits.obsolete"));
  }

  public HermesCounter committedCounter() {
    return HermesCounters.from(meterRegistry.counter("offset-commits.committed"));
  }

  public HermesTimer duration() {
    return HermesTimer.from(meterRegistry.timer("offset-commits.duration"));
  }

  public HermesCounter failuresCounter() {
    return HermesCounters.from(meterRegistry.counter("offset-commits.failures"));
  }
}
