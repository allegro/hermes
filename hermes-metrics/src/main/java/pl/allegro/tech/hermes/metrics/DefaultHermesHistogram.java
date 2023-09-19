package pl.allegro.tech.hermes.metrics;

public class DefaultHermesHistogram implements HermesHistogram {
    private final io.micrometer.core.instrument.DistributionSummary micrometerHistogram;
    private final com.codahale.metrics.Histogram graphiteHistogram;

    private DefaultHermesHistogram(io.micrometer.core.instrument.DistributionSummary micrometerHistogram,
                                   com.codahale.metrics.Histogram graphiteHistogram) {
        this.micrometerHistogram = micrometerHistogram;
        this.graphiteHistogram = graphiteHistogram;
    }

    public static DefaultHermesHistogram of(io.micrometer.core.instrument.DistributionSummary micrometerHistogram,
                                            com.codahale.metrics.Histogram graphiteHistogram) {
        return new DefaultHermesHistogram(micrometerHistogram, graphiteHistogram);
    }

    @Override
    public void record(long value) {
        micrometerHistogram.record(value);
        graphiteHistogram.update(value);
    }
}
