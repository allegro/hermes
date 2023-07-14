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

// exposes kafka producer metrics, see: https://docs.confluent.io/platform/current/kafka/monitoring.html#producer-metrics
public class Producers {
    private final Producer<byte[], byte[]> ackLeader;
    private final Producer<byte[], byte[]> ackAll;

    private final boolean reportNodeMetrics;
    private final AtomicBoolean nodeMetricsRegistered = new AtomicBoolean(false);

    public Producers(Producer<byte[], byte[]> ackLeader,
                     Producer<byte[], byte[]> ackAll,
                     boolean reportNodeMetrics) {
        this.ackLeader = ackLeader;
        this.ackAll = ackAll;
        this.reportNodeMetrics = reportNodeMetrics;
    }

    public Producer<byte[], byte[]> get(Topic topic) {
        return topic.isReplicationConfirmRequired() ? ackAll : ackLeader;
    }

    public void registerGauges(MetricsFacade metricsFacade) {
        MetricName bufferTotalBytes = new MetricName("buffer-total-bytes", "producer-metrics", "buffer total bytes", Collections.emptyMap());
        metricsFacade.producerMetrics().registerAckAllTotalBytesGauge(ackAll, producerGauge(bufferTotalBytes));
        metricsFacade.producerMetrics().registerAckLeaderTotalBytesGauge(ackLeader, producerGauge(bufferTotalBytes));

        MetricName bufferAvailableBytes = new MetricName("buffer-available-bytes", "producer-metrics", "buffer available bytes", Collections.emptyMap());
        metricsFacade.producerMetrics().registerAckAllAvailableBytesGauge(ackAll, producerGauge(bufferAvailableBytes));
        metricsFacade.producerMetrics().registerAckLeaderAvailableBytesGauge(ackLeader, producerGauge(bufferAvailableBytes));

        MetricName compressionRate = new MetricName("compression-rate-avg", "producer-metrics", "average compression rate", Collections.emptyMap());
        metricsFacade.producerMetrics().registerAckAllCompressionRateGauge(ackAll, producerGauge(compressionRate));
        metricsFacade.producerMetrics().registerAckLeaderCompressionRateGauge(ackLeader, producerGauge(compressionRate));

        MetricName failedBatches = new MetricName("record-error-total", "producer-metrics", "failed publishing batches", Collections.emptyMap());
        metricsFacade.producerMetrics().registerAckAllFailedBatchesGauge(ackAll, producerGauge(failedBatches));
        metricsFacade.producerMetrics().registerAckLeaderFailedBatchesGauge(ackLeader, producerGauge(failedBatches));

        MetricName metadataAge = new MetricName("metadata-age", "producer-metrics", "age [s] of metadata", Collections.emptyMap());
        metricsFacade.producerMetrics().registerAckAllMetadataAgeGauge(ackAll, producerGauge(metadataAge));
        metricsFacade.producerMetrics().registerAckLeaderMetadataAgeGauge(ackLeader, producerGauge(metadataAge));

        MetricName queueTimeMax = new MetricName("record-queue-time-max", "producer-metrics", "maximum time [ms] that batch spent in the send buffer", Collections.emptyMap());
        metricsFacade.producerMetrics().registerAckAllRecordQueueTimeMaxGauge(ackAll, producerGauge(queueTimeMax));
        metricsFacade.producerMetrics().registerAckLeaderRecordQueueTimeMaxGauge(ackLeader, producerGauge(queueTimeMax));
    }

    public void maybeRegisterNodeMetricsGauges(MetricsFacade metricsFacade) {
        if (reportNodeMetrics && nodeMetricsRegistered.compareAndSet(false, true)) {
            registerLatencyPerBrokerGauge(metricsFacade);
        }
    }

    private void registerLatencyPerBrokerGauge(MetricsFacade metricsFacade) {
        List<Node> brokers = ProducerBrokerNodeReader.read(ackLeader);
        for (Node broker : brokers) {
            metricsFacade.producerMetrics().registerAckAllMaxLatencyBrokerGauge(ackAll, producerLatencyGauge("request-latency-max", broker), broker.host());
            metricsFacade.producerMetrics().registerAckLeaderMaxLatencyPerBrokerGauge(ackLeader, producerLatencyGauge("request-latency-max", broker), broker.host());
            metricsFacade.producerMetrics().registerAckAllAvgLatencyPerBrokerGauge(ackAll, producerLatencyGauge("request-latency-avg", broker), broker.host());
            metricsFacade.producerMetrics().registerAckLeaderAvgLatencyPerBrokerGauge(ackLeader, producerLatencyGauge("request-latency-avg", broker), broker.host());
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

    public void close() {
        ackAll.close();
        ackLeader.close();
    }
}
