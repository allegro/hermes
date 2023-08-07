package pl.allegro.tech.hermes.metrics;

public class HermesHistogram {
    private final io.micrometer.core.instrument.DistributionSummary micrometerHistogram;
    private final com.codahale.metrics.Histogram graphiteHistogram;

    private HermesHistogram(io.micrometer.core.instrument.DistributionSummary micrometerHistogram,
                            com.codahale.metrics.Histogram graphiteHistogram) {
        this.micrometerHistogram = micrometerHistogram;
        this.graphiteHistogram = graphiteHistogram;
    }

    public static HermesHistogram of(io.micrometer.core.instrument.DistributionSummary micrometerHistogram,
                                   com.codahale.metrics.Histogram graphiteHistogram) {
        return new HermesHistogram(micrometerHistogram, graphiteHistogram);
    }

    public void record(int value) {
        micrometerHistogram.record(value);
        graphiteHistogram.update(value);
    }
}
