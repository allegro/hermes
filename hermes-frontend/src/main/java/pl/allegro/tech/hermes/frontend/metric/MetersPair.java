package pl.allegro.tech.hermes.frontend.metric;

import com.codahale.metrics.Meter;

public class MetersPair {
    private final Meter meter1;
    private final Meter meter2;

    public MetersPair(Meter meter1, Meter meter2) {
        this.meter1 = meter1;
        this.meter2 = meter2;
    }

    void mark() {
        meter1.mark();
        meter2.mark();
    }
}