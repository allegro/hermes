package pl.allegro.tech.hermes.metrics.counters;

public class HermesCounters {
  public static DefaultHermesCounter from(io.micrometer.core.instrument.Counter micrometerCounter) {
    return new DefaultHermesCounter(micrometerCounter);
  }
}
