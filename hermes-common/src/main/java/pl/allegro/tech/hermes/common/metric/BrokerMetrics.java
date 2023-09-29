package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

public class BrokerMetrics {
    private final MeterRegistry meterRegistry;

    public BrokerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordBrokerLatency(String broker, Duration duration) {
        Timer.builder("broker.latency")
                .tag("broker", broker)
                .publishPercentileHistogram()
                .maximumExpectedValue(Duration.ofSeconds(5))
                .register(meterRegistry)
                .record(duration);
    }
}
