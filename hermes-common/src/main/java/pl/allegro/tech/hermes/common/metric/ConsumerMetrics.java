package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import pl.allegro.tech.hermes.metrics.HermesTimer;

public class ConsumerMetrics {
    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;

    public ConsumerMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
    }

    public HermesTimer offsetCommitterDuration() {
        return HermesTimer.from(
                meterRegistry.timer("offset-committer.duration"),
                hermesMetrics.timer("offset-committer.duration")
        );
    }
}
