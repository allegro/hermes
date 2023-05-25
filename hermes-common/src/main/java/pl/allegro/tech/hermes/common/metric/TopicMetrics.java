package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.metrics.HermesTimer;

public class TopicMetrics {
    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;

    public TopicMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
    }

    public HermesTimer ackAllGlobalLatency() {
        return HermesTimer.from(
                meterRegistry.timer("ack-all.global-latency"),
                hermesMetrics.timer(Timers.ACK_ALL_LATENCY)
        );
    }

    public HermesTimer ackAllTopicLatency(TopicName topic) {
        return HermesTimer.from(
                timer("ack-all.topic-latency", topic),
                hermesMetrics.timer(Timers.ACK_ALL_TOPIC_LATENCY, topic));
    }

    public HermesTimer ackAllBrokerLatency() {
        return HermesTimer.from(
                meterRegistry.timer("ack-all.broker-latency"),
                hermesMetrics.timer(Timers.ACK_ALL_BROKER_LATENCY));
    }

    public HermesTimer ackLeaderGlobalLatency() {
        return HermesTimer.from(
                meterRegistry.timer("ack-leader.global-latency"),
                hermesMetrics.timer(Timers.ACK_LEADER_LATENCY));
    }

    public HermesTimer ackLeaderTopicLatency(TopicName topic) {
        return HermesTimer.from(
                timer("ack-leader.topic-latency", topic),
                hermesMetrics.timer(Timers.ACK_LEADER_TOPIC_LATENCY, topic));
    }

    public HermesTimer ackLeaderBrokerLatency() {
        return HermesTimer.from(
                meterRegistry.timer("ack-leader.broker-latency"),
                hermesMetrics.timer(Timers.ACK_LEADER_BROKER_LATENCY));
    }

    private Timer timer(String metricName, TopicName topicName) {
        return meterRegistry.timer(metricName, "group", topicName.getGroupName(), "topic", topicName.getName());
    }
}
