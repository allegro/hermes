package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;

public class MetricsFacade {

    private final TopicMetrics topicMetrics;
    private final SubscriptionMetrics subscriptionMetrics;
    private final ConsumerMetrics consumerMetrics;
    private final TrackerElasticSearchMetrics trackerElasticSearchMetrics;
    private final PersistentBufferMetrics persistentBufferMetrics;
    private final ProducerMetrics producerMetrics;
    private final ExecutorMetrics executorMetrics;
    private final SchemaClientMetrics schemaClientMetrics;
    private final UndeliveredMessagesMetrics undeliveredMessagesMetrics;
    private final DeserializationMetrics deserializationMetrics;

    public MetricsFacade(MeterRegistry meterRegistry,
                         HermesMetrics hermesMetrics) {
        this.topicMetrics = new TopicMetrics(hermesMetrics, meterRegistry);
        this.subscriptionMetrics = new SubscriptionMetrics(hermesMetrics, meterRegistry);
        this.consumerMetrics = new ConsumerMetrics(hermesMetrics, meterRegistry);
        this.trackerElasticSearchMetrics = new TrackerElasticSearchMetrics(hermesMetrics, meterRegistry);
        this.persistentBufferMetrics = new PersistentBufferMetrics(hermesMetrics, meterRegistry);
        this.producerMetrics = new ProducerMetrics(hermesMetrics, meterRegistry);
        this.executorMetrics = new ExecutorMetrics(hermesMetrics, meterRegistry);
        this.schemaClientMetrics = new SchemaClientMetrics(hermesMetrics, meterRegistry);
        this.undeliveredMessagesMetrics = new UndeliveredMessagesMetrics(hermesMetrics, meterRegistry);
        this.deserializationMetrics =  new DeserializationMetrics(hermesMetrics, meterRegistry);
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

    public TrackerElasticSearchMetrics trackerElasticSearch() {
        return trackerElasticSearchMetrics;
    }

    public PersistentBufferMetrics persistentBuffer() {
        return persistentBufferMetrics;
    }

    public ProducerMetrics producer() {
        return producerMetrics;
    }

    public ExecutorMetrics executor() {
        return executorMetrics;
    }

    public SchemaClientMetrics schemaClient() {
        return schemaClientMetrics;
    }

    public UndeliveredMessagesMetrics undeliveredMessages() {
        return undeliveredMessagesMetrics;
    }

    public DeserializationMetrics deserialization() {
        return deserializationMetrics;
    }
}

