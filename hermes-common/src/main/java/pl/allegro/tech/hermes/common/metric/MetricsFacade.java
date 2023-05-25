package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;

public class MetricsFacade {

    private final TopicMetrics topicMetrics;
    private final ConsumerMetrics consumerMetrics;

    public MetricsFacade(MeterRegistry meterRegistry,
                         HermesMetrics hermesMetrics) {
        this.topicMetrics = new TopicMetrics(hermesMetrics, meterRegistry);
        this.consumerMetrics = new ConsumerMetrics(hermesMetrics, meterRegistry);
    }

    public TopicMetrics topics() {
        return topicMetrics;
    }

    public ConsumerMetrics consumers() {
        return consumerMetrics;
    }
}

