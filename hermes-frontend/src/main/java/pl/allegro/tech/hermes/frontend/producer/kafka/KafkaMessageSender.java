package pl.allegro.tech.hermes.frontend.producer.kafka;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.record.RecordBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerLatencyReporter;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.metadata.ProduceMetadata;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;

public class KafkaMessageSender<K, V> {

  private static final Logger logger = LoggerFactory.getLogger(KafkaMessageSender.class);

  private final Producer<K, V> producer;
  private final BrokerLatencyReporter brokerLatencyReporter;
  private final String datacenter;
  private final ScheduledExecutorService chaosScheduler;

  KafkaMessageSender(
      Producer<K, V> kafkaProducer,
      BrokerLatencyReporter brokerLatencyReporter,
      String datacenter,
      ScheduledExecutorService chaosScheduler) {
    this.producer = kafkaProducer;
    this.brokerLatencyReporter = brokerLatencyReporter;
    this.datacenter = datacenter;
    this.chaosScheduler = chaosScheduler;
  }

  public String getDatacenter() {
    return datacenter;
  }

  public void send(
      ProducerRecord<K, V> producerRecord,
      CachedTopic cachedTopic,
      Message message,
      Callback callback,
      MultiDatacenterMessageProducer.ChaosExperiment experiment) {
    if (experiment.enabled()) {
      try {
        chaosScheduler.schedule(
            () -> {
              if (experiment.completeWithError()) {
                var exception =
                    new ChaosException(datacenter, experiment.delayInMillis(), message.getId());
                callback.onCompletion(exceptionalRecordMetadata(cachedTopic), exception);
              } else {
                send(producerRecord, cachedTopic, message, callback);
              }
            },
            experiment.delayInMillis(),
            TimeUnit.MILLISECONDS);
      } catch (RejectedExecutionException e) {
        logger.warn("Failed while scheduling chaos experiment. Sending message to Kafka.", e);
        send(producerRecord, cachedTopic, message, callback);
      }
    } else {
      send(producerRecord, cachedTopic, message, callback);
    }
  }

  public void send(
      ProducerRecord<K, V> producerRecord,
      CachedTopic cachedTopic,
      Message message,
      Callback callback) {
    HermesTimerContext timer = cachedTopic.startBrokerLatencyTimer();
    Callback meteredCallback = new MeteredCallback(timer, message, cachedTopic, callback);
    try {
      producer.send(producerRecord, meteredCallback);
    } catch (Exception e) {
      callback.onCompletion(exceptionalRecordMetadata(cachedTopic), e);
    }
  }

  private static RecordMetadata exceptionalRecordMetadata(CachedTopic cachedTopic) {
    var tp =
        new TopicPartition(
            cachedTopic.getKafkaTopics().getPrimary().name().asString(),
            RecordMetadata.UNKNOWN_PARTITION);
    return new RecordMetadata(tp, -1, -1, RecordBatch.NO_TIMESTAMP, -1, -1);
  }

  List<PartitionInfo> loadPartitionMetadataFor(String topic) {
    return producer.partitionsFor(topic);
  }

  public void close() {
    producer.close();
  }

  private Supplier<ProduceMetadata> produceMetadataSupplier(RecordMetadata recordMetadata) {
    return () -> {
      String kafkaTopicName = recordMetadata.topic();
      try {
        List<PartitionInfo> topicPartitions = producer.partitionsFor(kafkaTopicName);

        Optional<PartitionInfo> partitionInfo =
            topicPartitions.stream()
                .filter(p -> p.partition() == recordMetadata.partition())
                .findFirst();

        return partitionInfo
            .flatMap(partition -> Optional.ofNullable(partition.leader()))
            .map(Node::host)
            .map(ProduceMetadata::new)
            .orElse(ProduceMetadata.empty());
      } catch (InterruptException e) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        logger.warn(
            "Could not read information about partitions for topic {}. {}",
            kafkaTopicName,
            e.getMessage());
      }
      return ProduceMetadata.empty();
    };
  }

  private class MeteredCallback implements Callback {

    private final HermesTimerContext hermesTimerContext;
    private final Message message;
    private final CachedTopic cachedTopic;
    private final Callback callback;

    public MeteredCallback(
        HermesTimerContext hermesTimerContext,
        Message message,
        CachedTopic cachedTopic,
        Callback callback) {
      this.hermesTimerContext = hermesTimerContext;
      this.message = message;
      this.cachedTopic = cachedTopic;
      this.callback = callback;
    }

    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
      callback.onCompletion(metadata, exception);
      Supplier<ProduceMetadata> produceMetadataSupplier = produceMetadataSupplier(metadata);
      brokerLatencyReporter.report(
          hermesTimerContext, message, cachedTopic.getTopic().getAck(), produceMetadataSupplier);
    }
  }

  /**
   * Reads Apache Kafka Producer metric value by name and group.
   *
   * @param metricName the Kafka metric name (e.g. "buffer-total-bytes")
   * @param metricGroup the Kafka metric group (e.g. "producer-metrics")
   * @return the metric value as a double, or 0.0 if the metric is not found
   */
  double readProducerMetric(String metricName, String metricGroup) {
    Predicate<Map.Entry<MetricName, ? extends Metric>> predicate =
        entry ->
            entry.getKey().group().equals(metricGroup) && entry.getKey().name().equals(metricName);
    Optional<? extends Map.Entry<MetricName, ? extends Metric>> first =
        producer.metrics().entrySet().stream().filter(predicate).findFirst();
    Object value =
        first.map(metricNameEntry -> metricNameEntry.getValue().metricValue()).orElse(0.0d);
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    } else {
      return 0.0;
    }
  }
}
