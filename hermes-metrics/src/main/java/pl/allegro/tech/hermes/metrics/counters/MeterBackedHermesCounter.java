package pl.allegro.tech.hermes.metrics.counters;


import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesRateMeter;

public class MeterBackedHermesCounter implements HermesCounter, HermesRateMeter {
    protected final io.micrometer.core.instrument.Counter micrometerCounter;
    protected final com.codahale.metrics.Meter graphiteMeter = new com.codahale.metrics.Meter();

    protected MeterBackedHermesCounter(io.micrometer.core.instrument.Counter micrometerCounter) {
        this.micrometerCounter = micrometerCounter;
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
