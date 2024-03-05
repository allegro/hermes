package pl.allegro.tech.hermes.frontend.producer.kafka;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;

import java.util.List;
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

    public void registerLocalSenderMetrics(MetricsFacade metricsFacade) {
        ackLeader.registerGauges(metricsFacade, Topic.Ack.LEADER);
        ackAll.registerGauges(metricsFacade, Topic.Ack.ALL);
    }

    public static class Tuple {
        private final KafkaMessageSender<byte[], byte[]> ackLeader;
        private final KafkaMessageSender<byte[], byte[]> ackAll;

        public Tuple(KafkaMessageSender<byte[], byte[]> ackLeader, KafkaMessageSender<byte[], byte[]> ackAll) {
            this.ackLeader = ackLeader;
            this.ackAll = ackAll;
        }
    }

    public void close() {
        ackAll.close();
        ackLeader.close();
    }
}
