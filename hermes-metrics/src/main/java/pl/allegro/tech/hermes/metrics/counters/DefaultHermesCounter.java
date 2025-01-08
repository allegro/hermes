package pl.allegro.tech.hermes.metrics.counters;

import pl.allegro.tech.hermes.metrics.HermesCounter;

public class DefaultHermesCounter implements HermesCounter {
  protected final io.micrometer.core.instrument.Counter micrometerCounter;

  protected DefaultHermesCounter(io.micrometer.core.instrument.Counter micrometerCounter) {
    this.micrometerCounter = micrometerCounter;
  }

  public void increment(long size) {
    this.micrometerCounter.increment((double) size);
  }
}
