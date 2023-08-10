package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.metrics.DefaultHermesHistogram;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesHistogram;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.counters.HermesCounters;
import pl.allegro.tech.hermes.metrics.counters.MeterBackedHermesCounter;

import static pl.allegro.tech.hermes.common.metric.Meters.DELAYED_PROCESSING;

public class TopicMetrics {
    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;

    public TopicMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
    }

    public HermesTimer ackAllGlobalLatency() {
        return HermesTimer.from(
                meterRegistry.timer(TopicMetricsNames.TOPIC_ACK_ALL_GLOBAL_LATENCY),
                hermesMetrics.timer(Timers.ACK_ALL_LATENCY)
        );
    }

    public HermesTimer ackAllTopicLatency(TopicName topic) {
        return HermesTimer.from(
                micrometerTimer(TopicMetricsNames.TOPIC_ACK_ALL_LATENCY, topic),
                hermesMetrics.timer(Timers.ACK_ALL_TOPIC_LATENCY, topic));
    }

    public HermesTimer ackAllBrokerLatency() {
        return HermesTimer.from(
                meterRegistry.timer(TopicMetricsNames.TOPIC_ACK_ALL_BROKER_LATENCY),
                hermesMetrics.timer(Timers.ACK_ALL_BROKER_LATENCY));
    }

    public HermesTimer ackLeaderGlobalLatency() {
        return HermesTimer.from(
                meterRegistry.timer(TopicMetricsNames.TOPIC_ACK_LEADER_GLOBAL_LATENCY),
                hermesMetrics.timer(Timers.ACK_LEADER_LATENCY));
    }

    public HermesTimer ackLeaderTopicLatency(TopicName topic) {
        return HermesTimer.from(
                micrometerTimer(TopicMetricsNames.TOPIC_ACK_LEADER_LATENCY, topic),
                hermesMetrics.timer(Timers.ACK_LEADER_TOPIC_LATENCY, topic));
    }

    public HermesTimer ackLeaderBrokerLatency() {
        return HermesTimer.from(
                meterRegistry.timer(TopicMetricsNames.TOPIC_ACK_LEADER_BROKER_LATENCY),
                hermesMetrics.timer(Timers.ACK_LEADER_BROKER_LATENCY));
    }

    public MeterBackedHermesCounter topicThroughputBytes(TopicName topicName) {
        return HermesCounters.from(
                micrometerCounter(TopicMetricsNames.TOPIC_THROUGHPUT, topicName),
                hermesMetrics.meter(Meters.TOPIC_THROUGHPUT_BYTES, topicName)
        );
    }

    public MeterBackedHermesCounter topicGlobalThroughputBytes() {
        return HermesCounters.from(
                meterRegistry.counter(TopicMetricsNames.TOPIC_GLOBAL_THROUGHPUT),
                hermesMetrics.meter(Meters.THROUGHPUT_BYTES)
        );
    }

    public HermesCounter topicPublished(TopicName topicName) {
        return HermesCounters.from(
                micrometerCounter(TopicMetricsNames.TOPIC_PUBLISHED, topicName),
                hermesMetrics.counter(Counters.PUBLISHED, topicName)
        );
    }

    public HermesCounter topicGlobalRequestCounter() {
        return HermesCounters.from(
                meterRegistry.counter(TopicMetricsNames.TOPIC_GLOBAL_REQUESTS),
                hermesMetrics.meter(Meters.METER)
        );
    }

    public HermesCounter topicRequestCounter(TopicName topicName) {
        return HermesCounters.from(
                micrometerCounter(TopicMetricsNames.TOPIC_REQUESTS, topicName),
                hermesMetrics.meter(Meters.TOPIC_METER, topicName)
        );
    }

    public HermesCounter topicGlobalDelayedProcessingCounter() {
        return HermesCounters.from(
                meterRegistry.counter(TopicMetricsNames.TOPIC_GLOBAL_DELAYED_PROCESSING),
                hermesMetrics.meter(DELAYED_PROCESSING)
        );
    }

    public HermesCounter topicDelayedProcessingCounter(TopicName topicName) {
        return HermesCounters.from(
                micrometerCounter(TopicMetricsNames.TOPIC_DELAYED_PROCESSING, topicName),
                micrometerCounter("topic-delayed-processing", topicName),
                hermesMetrics.meter(Meters.TOPIC_DELAYED_PROCESSING, topicName)
        );
    }

    public HermesCounter topicGlobalHttpStatusCodeCounter(int statusCode) {
        return HermesCounters.from(
                meterRegistry.counter(TopicMetricsNames.TOPIC_GLOBAL_HTTP_STATUS_CODES, Tags.of("status_code", String.valueOf(statusCode))),
                hermesMetrics.httpStatusCodeMeter(statusCode)
        );
    }

    public HermesCounter topicHttpStatusCodeCounter(TopicName topicName, int statusCode) {
        return HermesCounters.from(
                meterRegistry.counter(TopicMetricsNames.TOPIC_HTTP_STATUS_CODES, topicTags(topicName)
                        .and("status_code", String.valueOf(statusCode))),
                hermesMetrics.httpStatusCodeMeter(statusCode, topicName)
        );
    }

    public HermesHistogram topicGlobalMessageContentSizeHistogram() {
        return DefaultHermesHistogram.of(
                DistributionSummary.builder(TopicMetricsNames.TOPIC_GLOBAL_MESSAGE_SIZE_BYTES)
                        .register(meterRegistry),
                hermesMetrics.messageContentSizeHistogram()
        );
    }

    public HermesHistogram topicMessageContentSizeHistogram(TopicName topicName) {
        return DefaultHermesHistogram.of(
                DistributionSummary.builder(TopicMetricsNames.TOPIC_MESSAGE_SIZE_BYTES)
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

    public static class TopicMetricsNames {
        public static final String TOPIC_ACK_ALL_GLOBAL_LATENCY = "topic.ack-all.global-latency";
        public static final String TOPIC_ACK_ALL_LATENCY = "topic.ack-all.latency";
        public static final String TOPIC_ACK_ALL_BROKER_LATENCY = "topic.ack-all.broker-latency";
        public static final String TOPIC_ACK_LEADER_GLOBAL_LATENCY = "topic.ack-leader.global-latency";
        public static final String TOPIC_ACK_LEADER_LATENCY = "topic.ack-leader.latency";
        public static final String TOPIC_ACK_LEADER_BROKER_LATENCY = "topic.ack-leader.broker-latency";
        public static final String TOPIC_THROUGHPUT = "topic.throughput";
        public static final String TOPIC_GLOBAL_THROUGHPUT = "topic.global-throughput";
        public static final String TOPIC_PUBLISHED = "topic.published";
        public static final String TOPIC_GLOBAL_REQUESTS = "topic.global-requests";
        public static final String TOPIC_REQUESTS = "topic.requests";
        public static final String TOPIC_GLOBAL_DELAYED_PROCESSING = "topic-global-delayed-processing";
        public static final String TOPIC_DELAYED_PROCESSING = "topic-delayed-processing";
        public static final String TOPIC_GLOBAL_HTTP_STATUS_CODES = "topic-global-http-status-codes";
        public static final String TOPIC_HTTP_STATUS_CODES = "topic-http-status-codes";
        public static final String TOPIC_GLOBAL_MESSAGE_SIZE_BYTES = "topic-global-message-size-bytes";
        public static final String TOPIC_MESSAGE_SIZE_BYTES = "topic-message-size-bytes";
    }
}
