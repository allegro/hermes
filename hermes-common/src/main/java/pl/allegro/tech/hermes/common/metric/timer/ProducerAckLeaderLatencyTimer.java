package pl.allegro.tech.hermes.common.metric.timer;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Timers;

public class ProducerAckLeaderLatencyTimer extends ProducerLatencyTimer {

    public ProducerAckLeaderLatencyTimer(HermesMetrics hermesMetrics, TopicName topicName) {
        super(hermesMetrics.timer(Timers.PRODUCER_ACK_LEADER_LATENCY), hermesMetrics.timer(Timers.PRODUCER_ACK_LEADER_TOPIC_LATENCY, topicName));
    }
}
