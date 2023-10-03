package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Collection;

import static pl.allegro.tech.hermes.common.metric.Counters.DELIVERED;
import static pl.allegro.tech.hermes.common.metric.Counters.DISCARDED;
import static pl.allegro.tech.hermes.common.metric.Counters.MAXRATE_FETCH_FAILURES;
import static pl.allegro.tech.hermes.common.metric.Counters.MAXRATE_RATE_HISTORY_FAILURES;
import static pl.allegro.tech.hermes.common.metric.Gauges.MAX_RATE_ACTUAL_RATE_VALUE;
import static pl.allegro.tech.hermes.common.metric.Gauges.MAX_RATE_VALUE;
import static pl.allegro.tech.hermes.common.metric.Gauges.OUTPUT_RATE;
import static pl.allegro.tech.hermes.common.metric.Meters.DISCARDED_SUBSCRIPTION_METER;
import static pl.allegro.tech.hermes.common.metric.Meters.FAILED_METER_SUBSCRIPTION;
import static pl.allegro.tech.hermes.common.metric.Meters.FILTERED_METER;
import static pl.allegro.tech.hermes.common.metric.Meters.SUBSCRIPTION_BATCH_METER;
import static pl.allegro.tech.hermes.common.metric.Meters.SUBSCRIPTION_METER;
import static pl.allegro.tech.hermes.common.metric.Meters.SUBSCRIPTION_THROUGHPUT_BYTES;
import static pl.allegro.tech.hermes.common.metric.SubscriptionTagsFactory.subscriptionTags;
import static pl.allegro.tech.hermes.common.metric.Timers.CONSUMER_IDLE_TIME;
import static pl.allegro.tech.hermes.common.metric.Timers.SUBSCRIPTION_LATENCY;

public class MetricsFacade {

    private final MeterRegistry meterRegistry;
    private final HermesMetrics hermesMetrics;
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
    private final WorkloadMetrics workloadMetrics;
    private final ConsumerSenderMetrics consumerSenderMetrics;
    private final OffsetCommitsMetrics offsetCommitsMetrics;
    private final MaxRateMetrics maxRateMetrics;
    private final BrokerMetrics brokerMetrics;

    public MetricsFacade(MeterRegistry meterRegistry, HermesMetrics hermesMetrics) {
        this.meterRegistry = meterRegistry;
        this.hermesMetrics = hermesMetrics;
        this.topicMetrics = new TopicMetrics(hermesMetrics, meterRegistry);
        this.subscriptionMetrics = new SubscriptionMetrics(hermesMetrics, meterRegistry);
        this.consumerMetrics = new ConsumerMetrics(hermesMetrics, meterRegistry);
        this.trackerElasticSearchMetrics = new TrackerElasticSearchMetrics(hermesMetrics, meterRegistry);
        this.persistentBufferMetrics = new PersistentBufferMetrics(hermesMetrics, meterRegistry);
        this.producerMetrics = new ProducerMetrics(hermesMetrics, meterRegistry);
        this.executorMetrics = new ExecutorMetrics(hermesMetrics, meterRegistry);
        this.schemaClientMetrics = new SchemaClientMetrics(hermesMetrics, meterRegistry);
        this.undeliveredMessagesMetrics = new UndeliveredMessagesMetrics(hermesMetrics, meterRegistry);
        this.deserializationMetrics = new DeserializationMetrics(hermesMetrics, meterRegistry);
        this.workloadMetrics = new WorkloadMetrics(hermesMetrics, meterRegistry);
        this.consumerSenderMetrics = new ConsumerSenderMetrics(hermesMetrics, meterRegistry);
        this.offsetCommitsMetrics = new OffsetCommitsMetrics(hermesMetrics, meterRegistry);
        this.maxRateMetrics = new MaxRateMetrics(hermesMetrics, meterRegistry);
        this.brokerMetrics = new BrokerMetrics(meterRegistry);
    }

    public TopicMetrics topics() {
        return topicMetrics;
    }

    public SubscriptionMetrics subscriptions() {
        return subscriptionMetrics;
    }

    public ConsumerMetrics consumer() {
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

    public WorkloadMetrics workload() {
        return workloadMetrics;
    }

    public ConsumerSenderMetrics consumerSender() {
        return consumerSenderMetrics;
    }

    public OffsetCommitsMetrics offsetCommits() {
        return offsetCommitsMetrics;
    }

    public MaxRateMetrics maxRate() {
        return maxRateMetrics;
    }

    public BrokerMetrics broker() {
        return brokerMetrics;
    }

    public void unregisterAllMetricsRelatedTo(SubscriptionName subscription) {
        Collection<Meter> meters = Search.in(meterRegistry)
                .tags(subscriptionTags(subscription))
                .meters();
        for (Meter meter : meters) {
            meterRegistry.remove(meter);
        }
        hermesMetrics.unregister(DISCARDED_SUBSCRIPTION_METER, subscription);
        hermesMetrics.unregister(FAILED_METER_SUBSCRIPTION, subscription);
        hermesMetrics.unregister(SUBSCRIPTION_BATCH_METER, subscription);
        hermesMetrics.unregister(SUBSCRIPTION_METER, subscription);
        hermesMetrics.unregister(DELIVERED, subscription);
        hermesMetrics.unregister(DISCARDED, subscription);
        hermesMetrics.unregisterInflightGauge(subscription);
        hermesMetrics.unregisterInflightTimeHistogram(subscription);
        hermesMetrics.unregisterConsumerErrorsTimeoutMeter(subscription);
        hermesMetrics.unregisterConsumerErrorsOtherMeter(subscription);
        hermesMetrics.unregisterStatusMeters(subscription);
        hermesMetrics.unregister(OUTPUT_RATE, subscription);
        hermesMetrics.unregister(MAX_RATE_ACTUAL_RATE_VALUE, subscription);
        hermesMetrics.unregister(MAX_RATE_VALUE, subscription);
        hermesMetrics.unregister(MAXRATE_FETCH_FAILURES, subscription);
        hermesMetrics.unregister(MAXRATE_RATE_HISTORY_FAILURES, subscription);
        hermesMetrics.unregister(CONSUMER_IDLE_TIME, subscription);
        hermesMetrics.unregister(FILTERED_METER, subscription);
        hermesMetrics.unregister(SUBSCRIPTION_LATENCY, subscription);
        hermesMetrics.unregister(SUBSCRIPTION_THROUGHPUT_BYTES, subscription);
    }
}
