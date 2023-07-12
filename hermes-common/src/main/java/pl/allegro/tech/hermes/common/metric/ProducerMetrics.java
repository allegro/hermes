package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.Node;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL_BUFFER_AVAILABLE_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL_BUFFER_TOTAL_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL_COMPRESSION_RATE;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL_FAILED_BATCHES_TOTAL;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL_METADATA_AGE;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL_RECORD_QUEUE_TIME_MAX;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER_BUFFER_AVAILABLE_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER_BUFFER_TOTAL_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER_COMPRESSION_RATE;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER_FAILED_BATCHES_TOTAL;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER_METADATA_AGE;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER_RECORD_QUEUE_TIME_MAX;
import static pl.allegro.tech.hermes.common.metric.HermesMetrics.escapeDots;

// exposes producer metrics, see: https://docs.confluent.io/platform/current/kafka/monitoring.html#producer-metrics
public class ProducerMetrics {
    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;

    public ProducerMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
    }

    public void registerAckAllTotalBytesGauge(Producer<byte[], byte[]> producer) {
        registerTotalBytesGauge(producer, ACK_ALL_BUFFER_TOTAL_BYTES);
    }

    public void registerAckLeaderTotalBytesGauge(Producer<byte[], byte[]> producer) {
        registerTotalBytesGauge(producer, ACK_LEADER_BUFFER_TOTAL_BYTES);
    }

    public void registerAckAllAvailableBytesGauge(Producer<byte[], byte[]> producer) {
        registerAvailableBytesGauge(producer, ACK_ALL_BUFFER_AVAILABLE_BYTES);
    }

    public void registerAckLeaderAvailableBytesGauge(Producer<byte[], byte[]> producer) {
        registerAvailableBytesGauge(producer, ACK_LEADER_BUFFER_AVAILABLE_BYTES);
    }

    public void registerAckAllCompressionRateGauge(Producer<byte[], byte[]> producer) {
        registerCompressionRateGauge(producer, ACK_ALL_COMPRESSION_RATE);
    }

    public void registerAckLeaderCompressionRateGauge(Producer<byte[], byte[]> producer) {
        registerCompressionRateGauge(producer, ACK_LEADER_COMPRESSION_RATE);
    }

    public void registerAckAllFailedBatchesGauge(Producer<byte[], byte[]> producer) {
        registerFailedBatchesGauge(producer, ACK_ALL_FAILED_BATCHES_TOTAL);
    }

    public void registerAckLeaderFailedBatchesGauge(Producer<byte[], byte[]> producer) {
        registerFailedBatchesGauge(producer, ACK_LEADER_FAILED_BATCHES_TOTAL);
    }

    public void registerAckAllMetadataAgeGauge(Producer<byte[], byte[]> producer) {
        registerMetadataAgeGauge(producer, ACK_ALL_METADATA_AGE);
    }

    public void registerAckLeaderMetadataAgeGauge(Producer<byte[], byte[]> producer) {
        registerMetadataAgeGauge(producer, ACK_LEADER_METADATA_AGE);
    }

    public void registerAckAllRecordQueueTimeMaxGauge(Producer<byte[], byte[]> producer) {
        registerRecordQueueTimeMaxGauge(producer, ACK_ALL_RECORD_QUEUE_TIME_MAX);
    }

    public void registerAckLeaderRecordQueueTimeMaxGauge(Producer<byte[], byte[]> producer) {
        registerRecordQueueTimeMaxGauge(producer, ACK_LEADER_RECORD_QUEUE_TIME_MAX);
    }

    public void registerAckAllMaxLatencyPerBrokerGauge(Producer<byte[], byte[]> producer, List<Node> brokers) {
        registerLatencyPerBrokerGauge(producer, "request-latency-max", ACK_ALL, brokers);
    }

    public void registerAckLeaderMaxLatencyPerBrokerGauge(Producer<byte[], byte[]> producer, List<Node> brokers) {
        registerLatencyPerBrokerGauge(producer, "request-latency-max", ACK_LEADER, brokers);
    }

    public void registerAckAllAvgLatencyPerBrokerGauge(Producer<byte[], byte[]> producer, List<Node> brokers) {
        registerLatencyPerBrokerGauge(producer, "request-latency-avg", ACK_ALL, brokers);
    }

    public void registerAckLeaderAvgLatencyPerBrokerGauge(Producer<byte[], byte[]> producer, List<Node> brokers) {
        registerLatencyPerBrokerGauge(producer, "request-latency-avg", ACK_LEADER, brokers);
    }

    private void registerTotalBytesGauge(Producer<byte[], byte[]> producer, String gauge) {
        registerProducerGauge(
                producer,
                new MetricName("buffer-total-bytes", "producer-metrics", "buffer total bytes", Collections.emptyMap()),
                gauge
        );
    }

    private void registerAvailableBytesGauge(Producer<byte[], byte[]> producer, String gauge) {
        registerProducerGauge(
                producer,
                new MetricName("buffer-available-bytes", "producer-metrics", "buffer available bytes", Collections.emptyMap()),
                gauge
        );
    }

    private void registerCompressionRateGauge(Producer<byte[], byte[]> producer, String gauge) {
        registerProducerGauge(
                producer,
                new MetricName("compression-rate-avg", "producer-metrics", "average compression rate", Collections.emptyMap()),
                gauge
        );
    }

    private void registerFailedBatchesGauge(Producer<byte[], byte[]> producer, String gauge) {
        registerProducerGauge(
                producer,
                new MetricName("record-error-total", "producer-metrics", "failed publishing batches", Collections.emptyMap()),
                gauge
        );
    }

    private void registerMetadataAgeGauge(Producer<byte[], byte[]> producer, String gauge) {
        registerProducerGauge(
                producer,
                new MetricName("metadata-age", "producer-metrics", "age [s] of metadata", Collections.emptyMap()),
                gauge
        );
    }

    public void registerRecordQueueTimeMaxGauge(Producer<byte[], byte[]> producer, String gauge) {
        registerProducerGauge(
                producer,
                new MetricName(
                        "record-queue-time-max",
                        "producer-metrics",
                        "maximum time [ms] that batch spent in the send buffer",
                        Collections.emptyMap()),
                gauge
        );
    }

    private void registerLatencyPerBrokerGauge(Producer<byte[], byte[]> producer,
                                               String metricName,
                                               String producerName,
                                               List<Node> brokers) {
        for (Node broker : brokers) {
            registerLatencyPerBrokerGauge(producer, metricName, producerName, broker);
        }
    }

    private void registerLatencyPerBrokerGauge(Producer<byte[], byte[]> producer,
                                               String metricName,
                                               String producerName,
                                               Node node) {
        String gauge = Gauges.KAFKA_PRODUCER + producerName + "." + metricName + "." + escapeDots(node.host());
        Predicate<Map.Entry<MetricName, ? extends Metric>> predicate = entry -> entry.getKey().group().equals("producer-node-metrics")
                && entry.getKey().name().equals(metricName)
                && entry.getKey().tags().containsValue("node-" + node.id());
        registerProducerGaugeGraphite(producer, gauge, predicate);
        registerProducerGaugePrometheus(producer, gauge, predicate, Tags.of("broker", node.host()));
    }


    private void registerProducerGauge(final Producer<byte[], byte[]> producer,
                                       final MetricName producerMetricName,
                                       final String gauge) {
        Predicate<Map.Entry<MetricName, ? extends Metric>> predicate = entry -> entry.getKey().group().equals(producerMetricName.group()) && entry.getKey().name().equals(producerMetricName.name());
        registerProducerGaugeGraphite(producer, gauge, predicate);
        registerProducerGaugePrometheus(producer, gauge, predicate, Tags.empty());
    }

    private double findProducerMetricByName(Producer<byte[], byte[]> producer, Predicate<Map.Entry<MetricName, ? extends Metric>> predicate) {
        Optional<? extends Map.Entry<MetricName, ? extends Metric>> first =
                producer.metrics().entrySet().stream().filter(predicate).findFirst();
        double value = first.map(metricNameEntry -> metricNameEntry.getValue().value()).orElse(0.0);
        return value < 0 ? 0.0 : value;
    }

    private void registerProducerGaugePrometheus(Producer<byte[], byte[]> producer, String gauge, Predicate<Map.Entry<MetricName, ? extends Metric>> predicate, Tags tags) {
        Gauge.builder(gauge, producer, p -> findProducerMetricByName(p, predicate))
                .tags(tags)
                .register(meterRegistry);
    }

    private void registerProducerGaugeGraphite(Producer<byte[], byte[]> producer, String gauge, Predicate<Map.Entry<MetricName, ? extends Metric>> predicate) {
        hermesMetrics.registerGauge(gauge, () -> findProducerMetricByName(producer, predicate));
    }


}
