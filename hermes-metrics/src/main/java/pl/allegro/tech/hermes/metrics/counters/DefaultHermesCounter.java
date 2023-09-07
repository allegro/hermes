package pl.allegro.tech.hermes.metrics.counters;


import pl.allegro.tech.hermes.metrics.HermesCounter;

public class DefaultHermesCounter implements HermesCounter {
    protected final io.micrometer.core.instrument.Counter micrometerCounter;
    protected final com.codahale.metrics.Counter graphiteCounter;

    protected DefaultHermesCounter(io.micrometer.core.instrument.Counter micrometerCounter,
                                   com.codahale.metrics.Counter graphiteCounter) {
        this.micrometerCounter = micrometerCounter;
        this.graphiteCounter = graphiteCounter;
    }

    public void increment(long size) {
        this.micrometerCounter.increment((double) size);
        this.graphiteCounter.inc(size);
    }
}
