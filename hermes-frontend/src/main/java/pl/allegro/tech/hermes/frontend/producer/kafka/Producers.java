package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.Node;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.Gauges;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static pl.allegro.tech.hermes.common.metric.HermesMetrics.escapeDots;

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
        metricsFacade.producerMetrics().registerAckAllTotalBytesGauge(ackAll);
        metricsFacade.producerMetrics().registerAckLeaderTotalBytesGauge(ackLeader);
        metricsFacade.producerMetrics().registerAckAllAvailableBytesGauge(ackAll);
        metricsFacade.producerMetrics().registerAckLeaderAvailableBytesGauge(ackLeader);
        metricsFacade.producerMetrics().registerAckAllCompressionRateGauge(ackAll);
        metricsFacade.producerMetrics().registerAckLeaderCompressionRateGauge(ackLeader);
        metricsFacade.producerMetrics().registerAckAllFailedBatchesGauge(ackAll);
        metricsFacade.producerMetrics().registerAckLeaderFailedBatchesGauge(ackLeader);
        metricsFacade.producerMetrics().registerAckAllMetadataAgeGauge(ackAll);
        metricsFacade.producerMetrics().registerAckLeaderMetadataAgeGauge(ackLeader);
        metricsFacade.producerMetrics().registerAckAllRecordQueueTimeMaxGauge(ackAll);
        metricsFacade.producerMetrics().registerAckLeaderRecordQueueTimeMaxGauge(ackAll);
    }

    public void maybeRegisterNodeMetricsGauges(MetricsFacade metricsFacade) {
        if (reportNodeMetrics && nodeMetricsRegistered.compareAndSet(false, true)) {
            registerLatencyPerBrokerGauge(metricsFacade);
        }
    }

    private void registerLatencyPerBrokerGauge(MetricsFacade metricsFacade) {
        List<Node> brokers = ProducerBrokerNodeReader.read(ackLeader);
        metricsFacade.producerMetrics().registerAckAllAvgLatencyPerBrokerGauge(ackAll, brokers);
        metricsFacade.producerMetrics().registerAckLeaderAvgLatencyPerBrokerGauge(ackLeader, brokers);
        metricsFacade.producerMetrics().registerAckAllMaxLatencyPerBrokerGauge(ackAll, brokers);
        metricsFacade.producerMetrics().registerAckLeaderMaxLatencyPerBrokerGauge(ackLeader, brokers);
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

        String gauge = Gauges.KAFKA_PRODUCER + producerName + "." + metricName + "." + escapeDots(node.host());
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
            return value < 0 ? 0.0 : value;
        });
    }

    public void close() {
        ackAll.close();
        ackLeader.close();
    }
}
