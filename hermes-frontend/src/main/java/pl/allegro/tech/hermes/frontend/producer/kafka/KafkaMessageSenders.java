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
public class KafkaMessageSenders {
    private final KafkaMessageSender<byte[], byte[]> ackLeader;
    private final KafkaMessageSender<byte[], byte[]> ackAll;

    private final List<KafkaMessageSender<byte[], byte[]>> remoteAckLeader;
    private final List<KafkaMessageSender<byte[], byte[]>> remoteAckAll;


    public KafkaMessageSenders(Tuple localSenders,
                               List<Tuple> remoteSenders) {
        this.ackLeader = localSenders.ackLeader;
        this.ackAll = localSenders.ackAll;
        this.remoteAckLeader = remoteSenders.stream().map(it -> it.ackLeader).collect(Collectors.toList());
        this.remoteAckAll = remoteSenders.stream().map(it -> it.ackAll).collect(Collectors.toList());
    }

    public KafkaMessageSender<byte[], byte[]> get(Topic topic) {
        return topic.isReplicationConfirmRequired() ? ackAll : ackLeader;
    }

    public List<KafkaMessageSender<byte[], byte[]>> getRemote(Topic topic) {
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


    private double findProducerMetric(KafkaMessageSender<byte[], byte[]> producer,
                                      Predicate<Map.Entry<MetricName, ? extends Metric>> predicate) {
        Optional<? extends Map.Entry<MetricName, ? extends Metric>> first =
                producer.metrics().entrySet().stream().filter(predicate).findFirst();
        double value = first.map(metricNameEntry -> metricNameEntry.getValue().value()).orElse(0.0);
        return value < 0 ? 0.0 : value;
    }


    private ToDoubleFunction<KafkaMessageSender<byte[], byte[]>> producerGauge(MetricName producerMetricName) {
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
        private final KafkaMessageSender<byte[], byte[]> ackLeader;
        private final KafkaMessageSender<byte[], byte[]> ackAll;

        public Tuple(KafkaMessageSender<byte[], byte[]> ackLeader, KafkaMessageSender<byte[], byte[]> ackAll) {
            this.ackLeader = ackLeader;
            this.ackAll = ackAll;
        }
    }
}
