package pl.allegro.tech.hermes.common.metric;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class MetricRegistryWithExponentiallyDecayingReservoir extends MetricRegistry {

    @Override
    public Histogram histogram(String name) {
        return histogram(name, () -> new Histogram(new ExponentiallyDecayingReservoir()));
    }

    @Override
    public Timer timer(String name) {
        return timer(name, () -> new Timer(new ExponentiallyDecayingReservoir()));
    }
}
