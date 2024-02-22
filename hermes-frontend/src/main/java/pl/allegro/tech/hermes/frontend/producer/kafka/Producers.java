package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.Node;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

// exposes kafka producer metrics, see: https://docs.confluent.io/platform/current/kafka/monitoring.html#producer-metrics
public class Producers {
    private final Producer<byte[], byte[]> ackLeader;
    private final Producer<byte[], byte[]> ackAll;

    private final Map<String, Producer<byte[], byte[]>> remoteAckLeader;
    private final Map<String, Producer<byte[], byte[]>> remoteAckAll;

    private final boolean reportNodeMetrics;
    private final AtomicBoolean nodeMetricsRegistered = new AtomicBoolean(false);

    public Producers(Tuple localProducers,
                     List<Tuple> remoteProducers,
                     boolean reportNodeMetrics) {
        this.ackLeader = localProducers.ackLeader;
        this.ackAll = localProducers.ackAll;
        this.reportNodeMetrics = reportNodeMetrics;
        this.remoteAckLeader = remoteProducers.stream()
                .collect(Collectors.toMap(e -> e.datacenter, e -> e.ackLeader));
        this.remoteAckAll = remoteProducers.stream()
                .collect(Collectors.toMap(e -> e.datacenter, e -> e.ackAll));
    }

    public Producer<byte[], byte[]> get(Topic topic) {
        return topic.isReplicationConfirmRequired() ? ackAll : ackLeader;
    }

    public Map<String, Producer<byte[], byte[]>> getRemote(Topic topic) {
        return topic.isReplicationConfirmRequired() ? remoteAckLeader : remoteAckAll;
    }

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

    public void maybeRegisterNodeMetricsGauges(MetricsFacade metricsFacade) {
        if (reportNodeMetrics && nodeMetricsRegistered.compareAndSet(false, true)) {
            registerLatencyPerBrokerGauge(metricsFacade);
        }
    }

    private void registerLatencyPerBrokerGauge(MetricsFacade metricsFacade) {
        List<Node> brokers = ProducerBrokerNodeReader.read(ackLeader);
        for (Node broker : brokers) {
            metricsFacade.producer().registerAckAllMaxLatencyBrokerGauge(ackAll,
                    producerLatencyGauge("request-latency-max", broker), broker.host());
            metricsFacade.producer().registerAckLeaderMaxLatencyPerBrokerGauge(ackLeader,
                    producerLatencyGauge("request-latency-max", broker), broker.host());
            metricsFacade.producer().registerAckAllAvgLatencyPerBrokerGauge(ackAll,
                    producerLatencyGauge("request-latency-avg", broker), broker.host());
            metricsFacade.producer().registerAckLeaderAvgLatencyPerBrokerGauge(ackLeader,
                    producerLatencyGauge("request-latency-avg", broker), broker.host());
        }
    }

    private double findProducerMetric(Producer<byte[], byte[]> producer,
                                      Predicate<Map.Entry<MetricName, ? extends Metric>> predicate) {
        Optional<? extends Map.Entry<MetricName, ? extends Metric>> first =
                producer.metrics().entrySet().stream().filter(predicate).findFirst();
        double value = first.map(metricNameEntry -> metricNameEntry.getValue().value()).orElse(0.0);
        return value < 0 ? 0.0 : value;
    }

    private ToDoubleFunction<Producer<byte[], byte[]>> producerLatencyGauge(String producerMetricName, Node node) {
        Predicate<Map.Entry<MetricName, ? extends Metric>> predicate = entry -> entry.getKey().group().equals("producer-node-metrics")
                && entry.getKey().name().equals(producerMetricName)
                && entry.getKey().tags().containsValue("node-" + node.id());
        return producer -> findProducerMetric(producer, predicate);
    }

    private ToDoubleFunction<Producer<byte[], byte[]>> producerGauge(MetricName producerMetricName) {
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
        private final String datacenter;
        private final Producer<byte[], byte[]> ackLeader;
        private final Producer<byte[], byte[]> ackAll;

        public Tuple(String datacenter, Producer<byte[], byte[]> ackLeader, Producer<byte[], byte[]> ackAll) {
            this.datacenter = datacenter;
            this.ackLeader = ackLeader;
            this.ackAll = ackAll;
        }
    }
}
