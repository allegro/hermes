package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.errors.InterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerLatencyReporter;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.metadata.ProduceMetadata;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

public class KafkaMessageSender<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageSender.class);

    private final Producer<K, V> producer;
    private final BrokerLatencyReporter brokerLatencyReporter;
    private final String datacenter;

    KafkaMessageSender(Producer<K, V> kafkaProducer, BrokerLatencyReporter brokerLatencyReporter, String datacenter) {
        this.producer = kafkaProducer;
        this.brokerLatencyReporter = brokerLatencyReporter;
        this.datacenter = datacenter;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void send(ProducerRecord<K, V> producerRecord,
                     CachedTopic cachedTopic,
                     Message message,
                     Callback callback) {
        HermesTimerContext timer = cachedTopic.startBrokerLatencyTimer();
        Callback meteredCallback = new MeteredCallback(timer, message, cachedTopic, callback);
        producer.send(producerRecord, meteredCallback);
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

                Optional<PartitionInfo> partitionInfo = topicPartitions.stream()
                        .filter(p -> p.partition() == recordMetadata.partition())
                        .findFirst();

                return partitionInfo.map(partition -> partition.leader().host())
                        .map(ProduceMetadata::new)
                        .orElse(ProduceMetadata.empty());
            } catch (InterruptException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.warn("Could not read information about partitions for topic {}. {}", kafkaTopicName, e.getMessage());
            }
            return ProduceMetadata.empty();
        };
    }

    private class MeteredCallback implements Callback {

        private final HermesTimerContext hermesTimerContext;
        private final Message message;
        private final CachedTopic cachedTopic;
        private final Callback callback;

        public MeteredCallback(HermesTimerContext hermesTimerContext, Message message, CachedTopic cachedTopic, Callback callback) {
            this.hermesTimerContext = hermesTimerContext;
            this.message = message;
            this.cachedTopic = cachedTopic;
            this.callback = callback;
        }

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            callback.onCompletion(metadata, exception);
            Supplier<ProduceMetadata> produceMetadataSupplier = produceMetadataSupplier(metadata);
            brokerLatencyReporter.report(hermesTimerContext, message, cachedTopic.getTopic().getAck(), produceMetadataSupplier);
        }
    }


    public void registerGauges(MetricsFacade metricsFacade, Topic.Ack ack) {
        MetricName bufferTotalBytes = producerMetric("buffer-total-bytes", "producer-metrics", "buffer total bytes");
        MetricName bufferAvailableBytes = producerMetric("buffer-available-bytes", "producer-metrics", "buffer available bytes");
        MetricName compressionRate = producerMetric("compression-rate-avg", "producer-metrics", "average compression rate");
        MetricName failedBatches = producerMetric("record-error-total", "producer-metrics", "failed publishing batches");
        MetricName metadataAge = producerMetric("metadata-age", "producer-metrics", "age [s] of metadata");
        MetricName queueTimeMax = producerMetric("record-queue-time-max", "producer-metrics", "maximum time [ms] that batch spent in the send buffer");

        // TODO: add 'datacenter' label
        if (ack == Topic.Ack.ALL) {
            metricsFacade.producer().registerAckAllTotalBytesGauge(producer, producerGauge(bufferTotalBytes));
            metricsFacade.producer().registerAckAllAvailableBytesGauge(producer, producerGauge(bufferAvailableBytes));
            metricsFacade.producer().registerAckAllCompressionRateGauge(producer, producerGauge(compressionRate));
            metricsFacade.producer().registerAckAllFailedBatchesGauge(producer, producerGauge(failedBatches));
            metricsFacade.producer().registerAckAllMetadataAgeGauge(producer, producerGauge(metadataAge));
            metricsFacade.producer().registerAckAllRecordQueueTimeMaxGauge(producer, producerGauge(queueTimeMax));
        } else if (ack == Topic.Ack.LEADER) {
            metricsFacade.producer().registerAckLeaderTotalBytesGauge(producer, producerGauge(bufferTotalBytes));
            metricsFacade.producer().registerAckLeaderAvailableBytesGauge(producer, producerGauge(bufferAvailableBytes));
            metricsFacade.producer().registerAckLeaderCompressionRateGauge(producer, producerGauge(compressionRate));
            metricsFacade.producer().registerAckLeaderFailedBatchesGauge(producer, producerGauge(failedBatches));
            metricsFacade.producer().registerAckLeaderMetadataAgeGauge(producer, producerGauge(metadataAge));
            metricsFacade.producer().registerAckLeaderRecordQueueTimeMaxGauge(producer, producerGauge(queueTimeMax));
        }
    }

    private double findProducerMetric(Producer<K, V> producer,
                                      Predicate<Map.Entry<MetricName, ? extends Metric>> predicate) {
        Optional<? extends Map.Entry<MetricName, ? extends Metric>> first =
                producer.metrics().entrySet().stream().filter(predicate).findFirst();
        double value = first.map(metricNameEntry -> metricNameEntry.getValue().value()).orElse(0.0);
        return value < 0 ? 0.0 : value;
    }

    private ToDoubleFunction<Producer<K, V>> producerGauge(MetricName producerMetricName) {
        Predicate<Map.Entry<MetricName, ? extends Metric>> predicate = entry -> entry.getKey().group().equals(producerMetricName.group())
                && entry.getKey().name().equals(producerMetricName.name());
        return producer -> findProducerMetric(producer, predicate);
    }

    private static MetricName producerMetric(String name, String group, String description) {
        return new MetricName(name, group, description, Collections.emptyMap());
    }

}
