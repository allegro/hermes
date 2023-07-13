package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesHistogram;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.HermesCounterWithRate;
import pl.allegro.tech.hermes.metrics.counters.HermesCounters;

import static pl.allegro.tech.hermes.common.metric.Meters.DELAYED_PROCESSING;
import static pl.allegro.tech.hermes.common.metric.Meters.METER;
import static pl.allegro.tech.hermes.common.metric.Meters.THROUGHPUT_BYTES;
import static pl.allegro.tech.hermes.common.metric.Meters.TOPIC_DELAYED_PROCESSING;
import static pl.allegro.tech.hermes.common.metric.Meters.TOPIC_METER;
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
                micrometerTimer("ack-all.topic-latency", topic),
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
                micrometerTimer("ack-leader.topic-latency", topic),
                hermesMetrics.timer(Timers.ACK_LEADER_TOPIC_LATENCY, topic));
    }

    public HermesTimer ackLeaderBrokerLatency() {
        return HermesTimer.from(
                meterRegistry.timer("ack-leader.broker-latency"),
                hermesMetrics.timer(Timers.ACK_LEADER_BROKER_LATENCY));
    }

    public HermesCounterWithRate topicThroughputBytes(TopicName topicName) {
        return HermesCounters.from(
                micrometerCounter("topic-throughput", topicName),
                hermesMetrics.meter(TOPIC_THROUGHPUT_BYTES, topicName)
        );
    }

    public HermesCounterWithRate topicGlobalThroughputBytes() {
        return HermesCounters.from(
                meterRegistry.counter("topic-global-throughput"),
                hermesMetrics.meter(THROUGHPUT_BYTES)
        );
    }

    public HermesCounter topicPublished(TopicName topicName) {
        return HermesCounters.from(
                micrometerCounter("published", topicName),
                hermesMetrics.counter(Counters.PUBLISHED, topicName)
        );
    }

    public HermesCounter topicGlobalRequestCounter() {
        return HermesCounters.from(
                meterRegistry.counter("topic-global-requests"),
                hermesMetrics.meter(METER)
        );
    }

    public HermesCounter topicRequestCounter(TopicName topicName) {
        return HermesCounters.from(
                micrometerCounter("topic-requests", topicName),
                hermesMetrics.meter(TOPIC_METER)
        );
    }

    public HermesCounter topicGlobalDelayedProcessingCounter() {
        return HermesCounters.from(
                meterRegistry.counter("topic-global-delayed-processing"),
                hermesMetrics.meter(DELAYED_PROCESSING)
        );
    }

    public HermesCounter topicDelayedProcessingCounter(TopicName topicName) {
        return HermesCounters.from(
                micrometerCounter("topic-delayed-processing", topicName),
                hermesMetrics.meter(TOPIC_DELAYED_PROCESSING)
        );
    }

    public HermesCounter topicGlobalHttpStatusCodeCounter(int statusCode) {
        return HermesCounters.from(
                meterRegistry.counter("topic-global-http-status-codes", Tags.of("status_code", String.valueOf(statusCode))),
                hermesMetrics.httpStatusCodeMeter(statusCode)
        );
    }

    public HermesCounter topicHttpStatusCodeCounter(TopicName topicName, int statusCode) {
        return HermesCounters.from(
                meterRegistry.counter("topic-http-status-codes", topicTags(topicName).and("status_code", String.valueOf(statusCode))),
                hermesMetrics.httpStatusCodeMeter(statusCode, topicName)
        );
    }

    public HermesHistogram topicGlobalMessageContentSizeHistogram() {
        return HermesHistogram.of(
                DistributionSummary.builder("topic-global-message-size")
                        .register(meterRegistry),
                hermesMetrics.messageContentSizeHistogram()
        );
    }

    public HermesHistogram topicMessageContentSizeHistogram(TopicName topicName) {
        return HermesHistogram.of(
                DistributionSummary.builder("topic-message-size")
                        .tags(topicTags(topicName))
                        .register(meterRegistry),
                hermesMetrics.messageContentSizeHistogram(topicName)
        );
    }

    private Timer micrometerTimer(String metricName, TopicName topicName) {
        return meterRegistry.timer(metricName, topicTags(topicName));
    }

    private Counter micrometerCounter(String metricName, TopicName topicName) {
        return meterRegistry.counter(metricName, topicTags(topicName));
    }

    private Tags topicTags(TopicName topicName) {
        return Tags.of(
                Tag.of("group", topicName.getGroupName()),
                Tag.of("topic", topicName.getName())
        );
    }
}
