package pl.allegro.tech.hermes.metrics.counters;

import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesCounterWithRate;

public class HermesCounters {
    public static HermesCounter from(io.micrometer.core.instrument.Counter micrometerCounter,
                                     com.codahale.metrics.Counter graphiteCounter) {
        return new DefaultHermesCounter(micrometerCounter, graphiteCounter);
    }

    public static HermesCounterWithRate from(io.micrometer.core.instrument.Counter micrometerCounter,
                                             com.codahale.metrics.Meter graphiteMeter) {
        return new DefaultHermesCounterWithRate(micrometerCounter, graphiteMeter);
    }
}
