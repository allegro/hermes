package pl.allegro.tech.hermes.common.metric.timer;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Timers;

public class ProducerAckAllLatencyTimer extends ProducerLatencyTimer {

    public ProducerAckAllLatencyTimer(HermesMetrics hermesMetrics, TopicName topicName) {
        super(hermesMetrics.timer(Timers.PRODUCER_ACK_ALL_LATENCY), hermesMetrics.timer(Timers.PRODUCER_ACK_ALL_TOPIC_LATENCY, topicName));
    }
}
