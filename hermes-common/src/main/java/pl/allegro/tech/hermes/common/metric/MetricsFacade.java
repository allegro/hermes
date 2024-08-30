package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.common.metric.SubscriptionTagsFactory.subscriptionTags;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import java.util.Collection;
import pl.allegro.tech.hermes.api.SubscriptionName;

public class MetricsFacade {

  private final MeterRegistry meterRegistry;
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
  private final ConsistencyMetrics consistencyMetrics;

  public MetricsFacade(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.topicMetrics = new TopicMetrics(meterRegistry);
    this.subscriptionMetrics = new SubscriptionMetrics(meterRegistry);
    this.consumerMetrics = new ConsumerMetrics(meterRegistry);
    this.trackerElasticSearchMetrics = new TrackerElasticSearchMetrics(meterRegistry);
    this.persistentBufferMetrics = new PersistentBufferMetrics(meterRegistry);
    this.producerMetrics = new ProducerMetrics(meterRegistry);
    this.executorMetrics = new ExecutorMetrics(meterRegistry);
    this.schemaClientMetrics = new SchemaClientMetrics(meterRegistry);
    this.undeliveredMessagesMetrics = new UndeliveredMessagesMetrics(meterRegistry);
    this.deserializationMetrics = new DeserializationMetrics(meterRegistry);
    this.workloadMetrics = new WorkloadMetrics(meterRegistry);
    this.consumerSenderMetrics = new ConsumerSenderMetrics(meterRegistry);
    this.offsetCommitsMetrics = new OffsetCommitsMetrics(meterRegistry);
    this.maxRateMetrics = new MaxRateMetrics(meterRegistry);
    this.brokerMetrics = new BrokerMetrics(meterRegistry);
    this.consistencyMetrics = new ConsistencyMetrics(meterRegistry);
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

  public ConsistencyMetrics consistency() {
    return consistencyMetrics;
  }

  public void unregisterAllMetricsRelatedTo(SubscriptionName subscription) {
    Collection<Meter> meters =
        Search.in(meterRegistry).tags(subscriptionTags(subscription)).meters();
    for (Meter meter : meters) {
      meterRegistry.remove(meter);
    }
  }
}
