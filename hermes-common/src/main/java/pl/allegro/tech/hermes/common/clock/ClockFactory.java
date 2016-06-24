package pl.allegro.tech.hermes.common.clock;

import org.glassfish.hk2.api.Factory;

import java.time.Clock;

public class ClockFactory implements Factory<Clock> {
    @Override
    public Clock provide() {
        return Clock.systemUTC();
    }

    @Override
    public void dispose(Clock instance) {

    }
}
