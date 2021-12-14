package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.codahale.metrics.Gauge;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.Node;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static pl.allegro.tech.hermes.common.metric.Gauges.PRODUCER_BUFFER_AVAILABLE_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.PRODUCER_BUFFER_TOTAL_BYTES;

public class Producers {
    private final Producer<byte[], byte[]> leaderConfirms;
    private final Producer<byte[], byte[]> everyoneConfirms;

    private final boolean reportNodeMetrics;
    private final AtomicBoolean nodeMetricsRegistered = new AtomicBoolean(false);

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
        registerProducerGauge(metrics, PRODUCER_BUFFER_TOTAL_BYTES);
        registerProducerGauge(metrics, PRODUCER_BUFFER_AVAILABLE_BYTES);
        registerProducerGauge(metrics, "compression-rate-avg");
        registerProducerGauge(metrics, "record-error-total");
        registerProducerGauge(metrics, "records-per-request-avg");
        registerProducerGauge(metrics, "requests-in-flight");
        registerProducerGauge(metrics, "record-queue-time-avg");
        registerProducerGauge(metrics, "record-queue-time-max");
    }

    private void registerProducerGauge(HermesMetrics metrics, String name) {
        metrics.registerProducerGaugeForAckLeader(name, new ProducerGauge(leaderConfirms, name));
        metrics.registerProducerGaugeForAckAll(name, new ProducerGauge(everyoneConfirms, name));
    }

    public void maybeRegisterNodeMetricsGauges(HermesMetrics metrics) {
        if (reportNodeMetrics && nodeMetricsRegistered.compareAndSet(false, true)) {
            registerLatencyPerBrokerGauge(metrics);
        }
    }

    private void registerLatencyPerBrokerGauge(HermesMetrics metrics) {
        List<Node> brokers = ProducerBrokerNodeReader.read(leaderConfirms);
        registerLatencyPerBrokerGauge(metrics, "request-latency-avg", brokers);
        registerLatencyPerBrokerGauge(metrics, "request-latency-max", brokers);
    }

    private void registerLatencyPerBrokerGauge(HermesMetrics metrics, String metricName, List<Node> brokers) {
        for (Node broker : brokers) {
            metrics.registerBrokerGaugeForAckLeader(metricName, broker.host(), new BrokerGauge(leaderConfirms, metricName, broker));
            metrics.registerBrokerGaugeForAckAll(metricName, broker.host(), new BrokerGauge(everyoneConfirms, metricName, broker));
        }
    }

    public void close() {
        everyoneConfirms.close();
        leaderConfirms.close();
    }

    private static class ProducerGauge implements Gauge<Double> {
        private static final String PRODUCER_METRICS_GROUP = "producer-metrics";

        private final Producer<byte[], byte[]> producer;
        private final String name;

        private ProducerGauge(Producer<byte[], byte[]> producer, String name) {
            this.producer = producer;
            this.name = name;
        }

        @Override
        public Double getValue() {
            return producer.metrics().entrySet().stream()
                    .filter(entry -> entry.getKey().group().equals(PRODUCER_METRICS_GROUP) && entry.getKey().name().equals(name))
                    .map(entry -> entry.getValue().value())
                    .filter(value -> value > 0)
                    .findFirst()
                    .orElse(0.0);
        }
    }

    private static class BrokerGauge implements Gauge<Double> {
        private final Producer<byte[], byte[]> producer;
        private final String name;
        private final Node node;

        private BrokerGauge(Producer<byte[], byte[]> producer, String name, Node node) {
            this.producer = producer;
            this.name = name;
            this.node = node;
        }

        @Override
        public Double getValue() {
            return producer.metrics().entrySet().stream()
                    .filter(entry ->
                            entry.getKey().group().equals("producer-node-metrics") &&
                                    entry.getKey().name().equals(name) &&
                                    entry.getKey().tags().containsValue("node-" + node.id())
                    )
                    .map(entry -> entry.getValue().value())
                    .filter(value -> value > 0)
                    .findFirst()
                    .orElse(0.0);
        }
    }
}
