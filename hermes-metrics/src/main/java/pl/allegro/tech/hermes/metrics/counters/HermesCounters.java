package pl.allegro.tech.hermes.metrics.counters;


public class HermesCounters {
    public static DefaultHermesCounter from(io.micrometer.core.instrument.Counter micrometerCounter) {
        return new DefaultHermesCounter(micrometerCounter);
    }

    public static MeterBackedHermesCounter from(io.micrometer.core.instrument.Counter micrometerCounter,
                                             com.codahale.metrics.Meter graphiteMeter) {
        return new MeterBackedHermesCounter(micrometerCounter, graphiteMeter);
    }
}
