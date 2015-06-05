package pl.allegro.tech.hermes.common.metric.timer;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Timers;

public class BrokerAckAllLatencyTimer extends BrokerLatencyTimer {

    public BrokerAckAllLatencyTimer(HermesMetrics hermesMetrics, TopicName topicName) {
        super(hermesMetrics.timer(Timers.PRODUCER_ACK_ALL_BROKER_LATENCY),
                hermesMetrics.timer(Timers.PRODUCER_ACK_ALL_BROKER_TOPIC_LATENCY, topicName));
    }
}
