package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesTimer;

import java.util.List;

import static pl.allegro.tech.hermes.common.metric.Meters.THROUGHPUT_BYTES;
import static pl.allegro.tech.hermes.common.metric.Meters.TOPIC_THROUGHPUT_BYTES;

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

    public HermesCounter topicThroughputBytes(TopicName topicName) {
        return HermesCounter.from(
                counter("topic-throughput", topicName),
                hermesMetrics.meter(TOPIC_THROUGHPUT_BYTES, topicName)
        );
    }

    public HermesCounter topicGlobalThroughputBytes() {
        return HermesCounter.from(
                meterRegistry.counter("topic-global-throughput"),
                hermesMetrics.meter(THROUGHPUT_BYTES)
        );
    }

    private Timer timer(String metricName, TopicName topicName) {
        return meterRegistry.timer(metricName, topicTags(topicName));
    }

    private Counter counter(String metricName, TopicName topicName) {
        return meterRegistry.counter(metricName, topicTags(topicName));
    }

    private Iterable<Tag> topicTags(TopicName topicName) {
        return List.of(
                Tag.of("group", topicName.getName()),
                Tag.of("topic", topicName.getName())
        );
    }
}
