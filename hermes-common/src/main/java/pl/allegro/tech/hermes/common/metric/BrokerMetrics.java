package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

public class BrokerMetrics {
    private final MeterRegistry meterRegistry;

    public BrokerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void registerBrokerLatency(String broker, Duration duration) {
        Timer.builder("broker.latency")
                .tag("broker", broker)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(duration);
    }
}
