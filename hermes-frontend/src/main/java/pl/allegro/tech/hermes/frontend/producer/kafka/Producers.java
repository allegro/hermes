package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.Gauges;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;


public class Producers {
    private final Producer<byte[], byte[]> leaderConfirms;
    private final Producer<byte[], byte[]> everyoneConfirms;

    private int brokerCount;
    private boolean reportNodeMetrics;

    public Producers(Producer<byte[], byte[]> leaderConfirms,
                     Producer<byte[], byte[]> everyoneConfirms,
                     ConfigFactory configFactory) {
        this.leaderConfirms = leaderConfirms;
        this.everyoneConfirms = everyoneConfirms;
        this.reportNodeMetrics = configFactory.getBooleanProperty(Configs.KAFKA_PRODUCER_REPORT_NODE_METRICS);
        this.brokerCount = configFactory.getStringProperty(Configs.KAFKA_BROKER_LIST).split(",").length;
    }

    public Producer<byte[], byte[]> get(Topic topic) {
        return topic.isReplicationConfirmRequired() ? everyoneConfirms : leaderConfirms;
    }

    public void registerGauges(HermesMetrics metrics) {
        registerTotalBytesGauge(leaderConfirms, metrics, Gauges.PRODUCER_LEADER_CONFIRMS_BUFFER_TOTAL_BYTES);
        registerAvailableBytesGauge(leaderConfirms, metrics, Gauges.PRODUCER_LEADER_CONFIRMS_BUFFER_AVAILABLE_BYTES);
        registerTotalBytesGauge(everyoneConfirms, metrics, Gauges.PRODUCER_EVERYONE_CONFIRMS_BUFFER_TOTAL_BYTES);
        registerAvailableBytesGauge(everyoneConfirms, metrics, Gauges.PRODUCER_EVERYONE_CONFIRMS_BUFFER_AVAILABLE_BYTES);
        registerCompressionRateGauge(leaderConfirms, metrics, Gauges.PRODUCER_LEADER_CONFIRMS_COMPRESSION_RATE);
        registerCompressionRateGauge(everyoneConfirms, metrics, Gauges.PRODUCER_EVERYONE_CONFIRMS_COMPRESSION_RATE);
        if (reportNodeMetrics) {
            registerLatencyPerBrokerGauge(metrics);
        }
    }

    private void registerLatencyPerBrokerGauge(HermesMetrics metrics) {
        registerLatencyPerBrokerGauge(everyoneConfirms, metrics, "request-latency-avg", "everyone-confirms");
        registerLatencyPerBrokerGauge(leaderConfirms, metrics, "request-latency-avg", "leader-confirms");
        registerLatencyPerBrokerGauge(everyoneConfirms, metrics, "request-latency-max", "everyone-confirms");
        registerLatencyPerBrokerGauge(leaderConfirms, metrics, "request-latency-max", "leader-confirms");
    }

    private void registerCompressionRateGauge(Producer<byte[], byte[]> producer, HermesMetrics metrics, String gauge) {
        registerProducerGauge(producer, metrics, new MetricName("compression-rate-avg", "producer-metrics"), gauge);
    }

    private void registerTotalBytesGauge(Producer<byte[], byte[]> producer, HermesMetrics metrics, String gauge) {
        registerProducerGauge(producer, metrics, new MetricName("buffer-total-bytes", "producer-metrics"), gauge);
    }

    private void registerAvailableBytesGauge(Producer<byte[], byte[]> producer, HermesMetrics metrics, String gauge) {
        registerProducerGauge(producer, metrics, new MetricName("buffer-available-bytes", "producer-metrics"), gauge);
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
                                               String metricName, String producerName) {
        for (int i=0; i<brokerCount; i++) {
            registerLatencyPerBrokerGauge(producer, metrics, metricName, producerName, "node-" + i);
        }
    }

    private void registerLatencyPerBrokerGauge(Producer<byte[], byte[]> producer,
                                               HermesMetrics metrics,
                                               String metricName,
                                               String producerName,
                                               String node) {

        String gauge = Gauges.PRODUCER_JMX_PREFIX + "." + producerName + "-" + metricName + "." + node;
        registerGauge(producer, metrics, gauge,
                entry -> entry.getKey().group().equals("producer-node-metrics")
                        && entry.getKey().name().equals(metricName)
                        && entry.getKey().tags().containsValue(node));

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
