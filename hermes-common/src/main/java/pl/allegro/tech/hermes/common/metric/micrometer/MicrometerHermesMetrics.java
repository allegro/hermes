package pl.allegro.tech.hermes.common.metric.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import pl.allegro.tech.hermes.api.TopicName;

public class MicrometerHermesMetrics {

    private final MeterRegistry meterRegistry;

    public MicrometerHermesMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Timer timer(String metricName) {
        return meterRegistry.timer(metricName);
    }

    public Timer timer(String metricName, TopicName topicName) {
        return meterRegistry.timer(metricName, "group", topicName.getGroupName(), "topic", topicName.getName());
    }
}

