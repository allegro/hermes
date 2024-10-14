package pl.allegro.tech.hermes.frontend.metric;

import pl.allegro.tech.hermes.metrics.HermesCounter;

public class MetersPair {
  private final HermesCounter meter1;
  private final HermesCounter meter2;

  public MetersPair(HermesCounter meter1, HermesCounter meter2) {
    this.meter1 = meter1;
    this.meter2 = meter2;
  }

  void mark() {
    meter1.increment(1L);
    meter2.increment(1L);
  }
}
