package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

// exposes kafka producer metrics, see: https://docs.confluent.io/platform/current/kafka/monitoring.html#producer-metrics
public class Producers {
    private final KafkaProducer<byte[], byte[]> ackLeader;
    private final KafkaProducer<byte[], byte[]> ackAll;

    private final List<KafkaProducer<byte[], byte[]>> remoteAckLeader;
    private final List<KafkaProducer<byte[], byte[]>> remoteAckAll;


    public Producers(Tuple localProducers,
                     List<Tuple> remoteProducers) {
        this.ackLeader = localProducers.ackLeader;
        this.ackAll = localProducers.ackAll;
        this.remoteAckLeader = remoteProducers.stream().map(it -> it.ackLeader).collect(Collectors.toList());
        this.remoteAckAll = remoteProducers.stream().map(it -> it.ackAll).collect(Collectors.toList());
    }

    public KafkaProducer<byte[], byte[]> get(Topic topic) {
        return topic.isReplicationConfirmRequired() ? ackAll : ackLeader;
    }

    public List<KafkaProducer<byte[], byte[]>> getRemote(Topic topic) {
        return topic.isReplicationConfirmRequired() ? remoteAckLeader : remoteAckAll;
    }


    //TODO: should probably be split per DC
    public void registerGauges(MetricsFacade metricsFacade) {
        MetricName bufferTotalBytes = producerMetric("buffer-total-bytes", "producer-metrics", "buffer total bytes");
        metricsFacade.producer().registerAckAllTotalBytesGauge(ackAll, producerGauge(bufferTotalBytes));
        metricsFacade.producer().registerAckLeaderTotalBytesGauge(ackLeader, producerGauge(bufferTotalBytes));

        MetricName bufferAvailableBytes = producerMetric("buffer-available-bytes", "producer-metrics", "buffer available bytes");
        metricsFacade.producer().registerAckAllAvailableBytesGauge(ackAll, producerGauge(bufferAvailableBytes));
        metricsFacade.producer().registerAckLeaderAvailableBytesGauge(ackLeader, producerGauge(bufferAvailableBytes));

        MetricName compressionRate = producerMetric("compression-rate-avg", "producer-metrics", "average compression rate");
        metricsFacade.producer().registerAckAllCompressionRateGauge(ackAll, producerGauge(compressionRate));
        metricsFacade.producer().registerAckLeaderCompressionRateGauge(ackLeader, producerGauge(compressionRate));

        MetricName failedBatches = producerMetric("record-error-total", "producer-metrics", "failed publishing batches");
        metricsFacade.producer().registerAckAllFailedBatchesGauge(ackAll, producerGauge(failedBatches));
        metricsFacade.producer().registerAckLeaderFailedBatchesGauge(ackLeader, producerGauge(failedBatches));

        MetricName metadataAge = producerMetric("metadata-age", "producer-metrics", "age [s] of metadata");
        metricsFacade.producer().registerAckAllMetadataAgeGauge(ackAll, producerGauge(metadataAge));
        metricsFacade.producer().registerAckLeaderMetadataAgeGauge(ackLeader, producerGauge(metadataAge));

        MetricName queueTimeMax = producerMetric("record-queue-time-max", "producer-metrics",
                "maximum time [ms] that batch spent in the send buffer");
        metricsFacade.producer().registerAckAllRecordQueueTimeMaxGauge(ackAll, producerGauge(queueTimeMax));
        metricsFacade.producer().registerAckLeaderRecordQueueTimeMaxGauge(ackLeader, producerGauge(queueTimeMax));
    }


    private double findProducerMetric(KafkaProducer<byte[], byte[]> producer,
                                      Predicate<Map.Entry<MetricName, ? extends Metric>> predicate) {
        Optional<? extends Map.Entry<MetricName, ? extends Metric>> first =
                producer.metrics().entrySet().stream().filter(predicate).findFirst();
        double value = first.map(metricNameEntry -> metricNameEntry.getValue().value()).orElse(0.0);
        return value < 0 ? 0.0 : value;
    }


    private ToDoubleFunction<KafkaProducer<byte[], byte[]>> producerGauge(MetricName producerMetricName) {
        Predicate<Map.Entry<MetricName, ? extends Metric>> predicate = entry -> entry.getKey().group().equals(producerMetricName.group())
                && entry.getKey().name().equals(producerMetricName.name());
        return producer -> findProducerMetric(producer, predicate);
    }

    private static MetricName producerMetric(String name, String group, String description) {
        return new MetricName(name, group, description, Collections.emptyMap());
    }

    public void close() {
        ackAll.close();
        ackLeader.close();
    }

    public static class Tuple {
        private final KafkaProducer<byte[], byte[]> ackLeader;
        private final KafkaProducer<byte[], byte[]> ackAll;

        public Tuple(KafkaProducer<byte[], byte[]> ackLeader, KafkaProducer<byte[], byte[]> ackAll) {
            this.ackLeader = ackLeader;
            this.ackAll = ackAll;
        }
    }
}
