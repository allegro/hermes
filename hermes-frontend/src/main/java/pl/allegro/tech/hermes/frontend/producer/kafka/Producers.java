package pl.allegro.tech.hermes.frontend.producer.kafka;

import java.util.Collections;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.Node;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.Gauges;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER;
import static pl.allegro.tech.hermes.common.metric.HermesMetrics.escapeDots;


public class Producers {
    private final Producer<byte[], byte[]> leaderConfirms;
    private final Producer<byte[], byte[]> everyoneConfirms;

    private final boolean reportNodeMetrics;
    private final AtomicBoolean nodeMetricsRegistered = new AtomicBoolean(false);

    public Producers(Producer<byte[], byte[]> leaderConfirms,
                     Producer<byte[], byte[]> everyoneConfirms,
                     boolean reportNodeMetrics) {
        this.leaderConfirms = leaderConfirms;
        this.everyoneConfirms = everyoneConfirms;
        this.reportNodeMetrics = reportNodeMetrics;
    }

    public Producer<byte[], byte[]> get(Topic topic) {
        return topic.isReplicationConfirmRequired() ? everyoneConfirms : leaderConfirms;
    }

    public void registerGauges(HermesMetrics metrics) {
        registerTotalBytesGauge(leaderConfirms, metrics, Gauges.ACK_LEADER_BUFFER_TOTAL_BYTES);
        registerAvailableBytesGauge(leaderConfirms, metrics, Gauges.ACK_LEADER_BUFFER_AVAILABLE_BYTES);
        registerTotalBytesGauge(everyoneConfirms, metrics, Gauges.ACK_ALL_BUFFER_TOTAL_BYTES);
        registerAvailableBytesGauge(everyoneConfirms, metrics, Gauges.ACK_ALL_BUFFER_AVAILABLE_BYTES);
        registerCompressionRateGauge(leaderConfirms, metrics, Gauges.ACK_LEADER_COMPRESSION_RATE);
        registerCompressionRateGauge(everyoneConfirms, metrics, Gauges.ACK_ALL_COMPRESSION_RATE);
        registerFailedBatchesGauge(everyoneConfirms, metrics, Gauges.ACK_ALL_FAILED_BATCHES_TOTAL);
        registerFailedBatchesGauge(leaderConfirms, metrics, Gauges.ACK_LEADER_FAILED_BATCHES_TOTAL);
        registerMetadataAgeGauge(everyoneConfirms, metrics, Gauges.EVERYONE_CONFIRMS_METADATA_AGE);
        registerMetadataAgeGauge(leaderConfirms, metrics, Gauges.LEADER_CONFIRMS_METADATA_AGE);
        registerRecordQueueTimeMaxGauge(everyoneConfirms, metrics, Gauges.EVERYONE_CONFIRMS_RECORD_QUEUE_TIME_MAX);
        registerRecordQueueTimeMaxGauge(leaderConfirms, metrics, Gauges.LEADER_CONFIRMS_RECORD_QUEUE_TIME_MAX);
    }

    public void maybeRegisterNodeMetricsGauges(HermesMetrics metrics) {
        if (reportNodeMetrics && nodeMetricsRegistered.compareAndSet(false, true)) {
            registerLatencyPerBrokerGauge(metrics);
        }
    }

    private void registerLatencyPerBrokerGauge(HermesMetrics metrics) {
        List<Node> brokers = ProducerBrokerNodeReader.read(leaderConfirms);
        registerLatencyPerBrokerGauge(everyoneConfirms, metrics, "request-latency-avg", ACK_ALL, brokers);
        registerLatencyPerBrokerGauge(leaderConfirms, metrics, "request-latency-avg", ACK_LEADER, brokers);
        registerLatencyPerBrokerGauge(everyoneConfirms, metrics, "request-latency-max", ACK_ALL, brokers);
        registerLatencyPerBrokerGauge(leaderConfirms, metrics, "request-latency-max", ACK_LEADER, brokers);
    }

    private void registerCompressionRateGauge(Producer<byte[], byte[]> producer, HermesMetrics metrics, String gauge) {
        registerProducerGauge(producer, metrics, new MetricName("compression-rate-avg", "producer-metrics", "average compression rate", Collections.emptyMap()), gauge);
    }

    private void registerTotalBytesGauge(Producer<byte[], byte[]> producer, HermesMetrics metrics, String gauge) {
        registerProducerGauge(producer, metrics, new MetricName("buffer-total-bytes", "producer-metrics", "buffer total bytes", Collections.emptyMap()), gauge);
    }

    private void registerAvailableBytesGauge(Producer<byte[], byte[]> producer, HermesMetrics metrics, String gauge) {
        registerProducerGauge(producer, metrics, new MetricName("buffer-available-bytes", "producer-metrics", "buffer available bytes", Collections.emptyMap()), gauge);
    }

    private void registerFailedBatchesGauge(Producer<byte[], byte[]> producer, HermesMetrics metrics, String gauge) {
        registerProducerGauge(producer, metrics, new MetricName("record-error-total", "producer-metrics", "failed publishing batches", Collections.emptyMap()), gauge);
    }

    private void registerRecordQueueTimeMaxGauge(Producer<byte[], byte[]> producer, HermesMetrics metrics, String gauge) {
        registerProducerGauge(producer, metrics, new MetricName("record-queue-time-max", "producer-metrics", "maximum time [ms] that batch spent in the send buffer", Collections.emptyMap()), gauge);
    }

    private void registerMetadataAgeGauge(Producer<byte[], byte[]> producer, HermesMetrics metrics, String gauge) {
        registerProducerGauge(producer, metrics, new MetricName("metadata-age", "producer-metrics", "age [s] of metadata", Collections.emptyMap()), gauge);
    }

    private void registerProducerGauge(final Producer<byte[], byte[]> producer,
                                       final HermesMetrics metrics,
                                       final MetricName name,
                                       final String gauge) {

        registerGauge(producer, metrics, gauge,
                entry -> entry.getKey().group().equals(name.group()) && entry.getKey().name().equals(name.name()));
    }

    private void registerLatencyPerBrokerGauge(Producer<byte[], byte[]> producer,
                                               HermesMetrics metrics,
                                               String metricName,
                                               String producerName,
                                               List<Node> brokers) {
        for (Node broker : brokers) {
            registerLatencyPerBrokerGauge(producer, metrics, metricName, producerName, broker);
        }
    }

    private void registerLatencyPerBrokerGauge(Producer<byte[], byte[]> producer,
                                               HermesMetrics metrics,
                                               String metricName,
                                               String producerName,
                                               Node node) {

        String gauge = Gauges.KAFKA_PRODUCER + "." + producerName + "." + metricName + "." + escapeDots(node.host());
        registerGauge(producer, metrics, gauge,
                entry -> entry.getKey().group().equals("producer-node-metrics")
                        && entry.getKey().name().equals(metricName)
                        && entry.getKey().tags().containsValue("node-" + node.id()));

    }

    private void registerGauge(Producer<byte[], byte[]> producer, HermesMetrics metrics, String gauge,
                               Predicate<Map.Entry<MetricName, ? extends Metric>> predicate) {
        metrics.registerGauge(gauge, () -> {
            Optional<? extends Map.Entry<MetricName, ? extends Metric>> first =
                    producer.metrics().entrySet().stream().filter(predicate).findFirst();
            double value = first.map(metricNameEntry -> metricNameEntry.getValue().value()).orElse(0.0);
            return value < 0? 0.0 : value;
        });
    }

    public void close() {
        everyoneConfirms.close();
        leaderConfirms.close();
    }
}
