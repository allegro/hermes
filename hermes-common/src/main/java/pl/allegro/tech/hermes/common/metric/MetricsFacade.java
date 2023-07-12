package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;

public class MetricsFacade {

    private final TopicMetrics topicMetrics;
    private final SubscriptionMetrics subscriptionMetrics;
    private final ConsumerMetrics consumerMetrics;
    private final BufferMetrics bufferMetrics;
    private final ProducerMetrics producerMetrics;

    public MetricsFacade(MeterRegistry meterRegistry,
                         HermesMetrics hermesMetrics) {
        this.topicMetrics = new TopicMetrics(hermesMetrics, meterRegistry);
        this.subscriptionMetrics = new SubscriptionMetrics(hermesMetrics, meterRegistry);
        this.consumerMetrics = new ConsumerMetrics(hermesMetrics, meterRegistry);
        this.bufferMetrics = new BufferMetrics(hermesMetrics, meterRegistry);
        this.producerMetrics = new ProducerMetrics(hermesMetrics, meterRegistry);
    }

    public TopicMetrics topics() {
        return topicMetrics;
    }

    public SubscriptionMetrics subscriptionMetrics() {
        return subscriptionMetrics;
    }

    public ConsumerMetrics consumers() {
        return consumerMetrics;
    }

    public BufferMetrics bufferMetrics() {
        return bufferMetrics;
    }

    public ProducerMetrics producerMetrics() {
        return producerMetrics;
    }
}

