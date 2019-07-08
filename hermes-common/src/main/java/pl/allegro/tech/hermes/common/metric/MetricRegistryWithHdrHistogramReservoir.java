package pl.allegro.tech.hermes.common.metric;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.mpierce.metrics.reservoir.hdrhistogram.HdrHistogramResetOnSnapshotReservoir;

public class MetricRegistryWithHdrHistogramReservoir extends MetricRegistry {

    @Override
    public Histogram histogram(String name) {
        return histogram(name, () -> new Histogram(new HdrHistogramResetOnSnapshotReservoir()));
    }

    @Override
    public Timer timer(String name) {
        return timer(name, () -> new Timer(new HdrHistogramResetOnSnapshotReservoir()));
    }
}
