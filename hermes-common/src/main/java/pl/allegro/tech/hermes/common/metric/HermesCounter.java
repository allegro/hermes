package pl.allegro.tech.hermes.common.metric;


public class HermesCounter implements HermesRateMeter {
    protected final io.micrometer.core.instrument.Counter micrometerCounter;
    protected final com.codahale.metrics.Meter graphiteMeter;

    protected HermesCounter(io.micrometer.core.instrument.Counter micrometerCounter,
                          com.codahale.metrics.Meter graphiteMeter) {
        this.micrometerCounter = micrometerCounter;
        this.graphiteMeter = graphiteMeter;
    }

    public static HermesCounter from(io.micrometer.core.instrument.Counter micrometerCounter,
                                     com.codahale.metrics.Meter graphiteMeter) {
        return new HermesCounter(micrometerCounter, graphiteMeter);
    }

    public void increment(long size) {
        this.micrometerCounter.increment((double) size);
        this.graphiteMeter.mark(size);
    }

    @Override
    public double getOneMinuteRate() {
        return this.graphiteMeter.getOneMinuteRate();
    }
}
