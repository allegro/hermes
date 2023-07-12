package pl.allegro.tech.hermes.metrics.counters;


import pl.allegro.tech.hermes.metrics.HermesCounterWithRate;

public class DefaultHermesCounterWithRate implements HermesCounterWithRate {
    protected final io.micrometer.core.instrument.Counter micrometerCounter;
    protected final com.codahale.metrics.Meter graphiteMeter;

    protected DefaultHermesCounterWithRate(io.micrometer.core.instrument.Counter micrometerCounter,
                                    com.codahale.metrics.Meter graphiteMeter) {
        this.micrometerCounter = micrometerCounter;
        this.graphiteMeter = graphiteMeter;
    }

    public static DefaultHermesCounterWithRate from(io.micrometer.core.instrument.Counter micrometerCounter,
                                             com.codahale.metrics.Meter graphiteMeter) {
        return new DefaultHermesCounterWithRate(micrometerCounter, graphiteMeter);
    }

    public void increment(long size) {
        this.micrometerCounter.increment((double) size);
        this.graphiteMeter.mark(size);
    }

    @Override
    public double getOneMinuteRate() {
        return graphiteMeter.getOneMinuteRate();
    }
}
