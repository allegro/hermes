package pl.allegro.tech.hermes.common.metric.timer;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Timers;

public class BrokerAckLeaderLatencyTimer extends BrokerLatencyTimer {

    public BrokerAckLeaderLatencyTimer(HermesMetrics hermesMetrics, TopicName topicName) {
        super(hermesMetrics.timer(Timers.ACK_LEADER_BROKER_LATENCY),
                hermesMetrics.timer(Timers.ACK_LEADER_BROKER_TOPIC_LATENCY, topicName));
    }
}
