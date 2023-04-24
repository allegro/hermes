package pl.allegro.tech.hermes.metrics;

import io.micrometer.core.instrument.Clock;

public class HermesTimer {
    private final io.micrometer.core.instrument.Timer micrometerTimer;
    private final com.codahale.metrics.Timer graphiteTimer;

    private HermesTimer(io.micrometer.core.instrument.Timer micrometerTimer, com.codahale.metrics.Timer graphiteTimer, Clock clock) {
        this.micrometerTimer = micrometerTimer;
        this.graphiteTimer = graphiteTimer;
    }

    public static HermesTimer from(io.micrometer.core.instrument.Timer micrometerTimer, com.codahale.metrics.Timer graphiteTimer) {
        return new HermesTimer(micrometerTimer, graphiteTimer, Clock.SYSTEM);
    }

    public HermesTimerContext time() {
        return HermesTimerContext.from(micrometerTimer, graphiteTimer);
    }
}