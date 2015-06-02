package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.Producer;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.Gauges;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;


public class Producers {
    private final Producer<byte[], byte[]> leaderConfirms;
    private final Producer<byte[], byte[]> everyoneConfirms;

    public Producers(Producer<byte[], byte[]> leaderConfirms, Producer<byte[], byte[]> everyoneConfirms) {
        this.leaderConfirms = leaderConfirms;
        this.everyoneConfirms = everyoneConfirms;
    }

    public Producer<byte[], byte[]> get(Topic topic) {
        return topic.isReplicationConfirmRequired() ? everyoneConfirms : leaderConfirms;
    }

    public void registerGauges(HermesMetrics metrics) {
        registerTotalBytesGauge(leaderConfirms, metrics, Gauges.PRODUCER_LEADER_CONFIRMS_BUFFER_TOTAL_BYTES);
        registerAvailableBytesGauge(leaderConfirms, metrics, Gauges.PRODUCER_LEADER_CONFIRMS_BUFFER_AVAILABLE_BYTES);
        registerTotalBytesGauge(everyoneConfirms, metrics, Gauges.PRODUCER_EVERYONE_CONFIRMS_BUFFER_TOTAL_BYTES);
        registerAvailableBytesGauge(everyoneConfirms, metrics, Gauges.PRODUCER_EVERYONE_CONFIRMS_BUFFER_AVAILABLE_BYTES);
    }

    private void registerTotalBytesGauge(Producer<byte[], byte[]> producer, HermesMetrics metrics, String gauge) {
        registerProducerGauge(producer, metrics, "buffer-total-bytes", gauge);
    }

    private void registerAvailableBytesGauge(Producer<byte[], byte[]> producer, HermesMetrics metrics, String gauge) {
        registerProducerGauge(producer, metrics, "buffer-available-bytes", gauge);
    }

    private void registerProducerGauge(final Producer<byte[], byte[]> producer,
                                       final HermesMetrics metrics,
                                       final String name,
                                       final String gauge) {

        metrics.registerGauge(gauge, () -> producer.metrics().containsKey(name) ? producer.metrics().get(name).value() : 0.0);
    }

    public void close() {
        everyoneConfirms.close();
        leaderConfirms.close();
    }
}
