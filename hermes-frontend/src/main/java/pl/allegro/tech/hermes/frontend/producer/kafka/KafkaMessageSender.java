package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public class KafkaMessageSender<K, V> {

    private final Producer<K, V> producer;
    private final String datacenter;

    KafkaMessageSender(Producer<K, V> kafkaProducer, String datacenter) {
        this.producer = kafkaProducer;
        this.datacenter = datacenter;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void send(ProducerRecord<K, V> producerRecord, Callback callback) {
        producer.send(producerRecord, callback);
    }

    List<PartitionInfo> loadPartitionMetadataFor(String topic) {
        return producer.partitionsFor(topic);
    }

    public void close() {
        producer.close();
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
