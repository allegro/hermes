package pl.allegro.tech.hermes.common.metric;

import pl.allegro.tech.hermes.api.TopicName;

public class ProducerAckLeaderLatencyTimer extends ProducerLatencyTimer {

    public ProducerAckLeaderLatencyTimer(HermesMetrics hermesMetrics, TopicName topicName) {
        super(hermesMetrics.timer(Timers.PRODUCER_ACK_LEADER_LATENCY), hermesMetrics.timer(Timers.PRODUCER_ACK_LEADER_TOPIC_LATENCY, topicName));
    }
}
