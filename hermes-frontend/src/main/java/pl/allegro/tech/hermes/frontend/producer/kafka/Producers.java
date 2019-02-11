package pl.allegro.tech.hermes.frontend.producer.kafka;

import java.util.Collections;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.Node;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.Gauges;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static pl.allegro.tech.hermes.common.metric.HermesMetrics.escapeDots;


public class Producers {
    private final Producer<byte[], byte[]> leaderConfirms;
    private final Producer<byte[], byte[]> everyoneConfirms;

    private boolean reportNodeMetrics;
    private AtomicBoolean nodeMetricsRegistered = new AtomicBoolean(false);

    public Producers(Producer<byte[], byte[]> leaderConfirms,
                     Producer<byte[], byte[]> everyoneConfirms,
                     ConfigFactory configFactory) {
        this.leaderConfirms = leaderConfirms;
        this.everyoneConfirms = everyoneConfirms;
        this.reportNodeMetrics = configFactory.getBooleanProperty(Configs.KAFKA_PRODUCER_REPORT_NODE_METRICS);
    }

    public Producer<byte[], byte[]> get(Topic topic) {
        return topic.isReplicationConfirmRequired() ? everyoneConfirms : leaderConfirms;
    }

    public void registerGauges(HermesMetrics metrics) {
        registerTotalBytesGauge(leaderConfirms, metrics, Gauges.LEADER_CONFIRMS_BUFFER_TOTAL_BYTES);
        registerAvailableBytesGauge(leaderConfirms, metrics, Gauges.LEADER_CONFIRMS_BUFFER_AVAILABLE_BYTES);
        registerTotalBytesGauge(everyoneConfirms, metrics, Gauges.EVERYONE_CONFIRMS_BUFFER_TOTAL_BYTES);
        registerAvailableBytesGauge(everyoneConfirms, metrics, Gauges.EVERYONE_CONFIRMS_BUFFER_AVAILABLE_BYTES);
        registerCompressionRateGauge(leaderConfirms, metrics, Gauges.LEADER_CONFIRMS_COMPRESSION_RATE);
        registerCompressionRateGauge(everyoneConfirms, metrics, Gauges.EVERYONE_CONFIRMS_COMPRESSION_RATE);
    }

    public void maybeRegisterNodeMetricsGauges(HermesMetrics metrics) {
        if (reportNodeMetrics && nodeMetricsRegistered.compareAndSet(false, true)) {
            registerLatencyPerBrokerGauge(metrics);
        }
    }

    private void registerLatencyPerBrokerGauge(HermesMetrics metrics) {
        List<Node> brokers = ProducerBrokerNodeReader.read(leaderConfirms);
        registerLatencyPerBrokerGauge(everyoneConfirms, metrics, "request-latency-avg", "everyone-confirms", brokers);
        registerLatencyPerBrokerGauge(leaderConfirms, metrics, "request-latency-avg", "leader-confirms", brokers);
        registerLatencyPerBrokerGauge(everyoneConfirms, metrics, "request-latency-max", "everyone-confirms", brokers);
        registerLatencyPerBrokerGauge(leaderConfirms, metrics, "request-latency-max", "leader-confirms", brokers);
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

        String gauge = Gauges.JMX_PREFIX + "." + producerName + "-" + metricName + "." + escapeDots(node.host());
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
            double value = first.isPresent() ? first.get().getValue().value() : 0.0;
            return value < 0? 0.0 : value;
        });
    }

    public void close() {
        everyoneConfirms.close();
        leaderConfirms.close();
    }
}
