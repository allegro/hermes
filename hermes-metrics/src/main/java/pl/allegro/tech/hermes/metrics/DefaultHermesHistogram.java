package pl.allegro.tech.hermes.metrics;

import io.micrometer.core.instrument.DistributionSummary;

public class DefaultHermesHistogram implements HermesHistogram {
  private final DistributionSummary micrometerHistogram;

  private DefaultHermesHistogram(
      io.micrometer.core.instrument.DistributionSummary micrometerHistogram) {
    this.micrometerHistogram = micrometerHistogram;
  }

  public static DefaultHermesHistogram of(
      io.micrometer.core.instrument.DistributionSummary micrometerHistogram) {
    return new DefaultHermesHistogram(micrometerHistogram);
  }

  @Override
  public void record(long value) {
    micrometerHistogram.record(value);
  }
}
