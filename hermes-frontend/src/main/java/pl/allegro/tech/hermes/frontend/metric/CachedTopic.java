package pl.allegro.tech.hermes.frontend.metric;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.common.metric.Timers;
import pl.allegro.tech.hermes.common.metric.micrometer.MicrometerHermesMetrics;
import pl.allegro.tech.hermes.common.metric.timer.StartedTimersPair;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachedTopic {

    private final Topic topic;
    private final KafkaTopics kafkaTopics;
    private final HermesMetrics hermesMetrics;
    private final boolean blacklisted;

    private final HermesTimer topicProducerLatencyTimer;
    private final HermesTimer globalProducerLatencyTimer;

    private final HermesTimer topicBrokerLatencyTimer;

    private final Meter globalRequestMeter;
    private final Meter topicRequestMeter;

    private final Meter globalDelayedProcessingMeter;
    private final Meter topicDelayedProcessingMeter;

    private final Histogram topicMessageContentSize;
    private final Histogram globalMessageContentSize;

    private final Meter topicThroughputMeter;
    private final Meter globalThroughputMeter;

    private final Counter published;

    private final Map<Integer, MetersPair> httpStatusCodesMeters = new ConcurrentHashMap<>();

    public CachedTopic(Topic topic, HermesMetrics hermesMetrics, MicrometerHermesMetrics micrometerHermesMetrics,
                       KafkaTopics kafkaTopics) {
        this(topic, hermesMetrics, micrometerHermesMetrics, kafkaTopics, false);
    }

    public CachedTopic(Topic topic, HermesMetrics oldHermesMetrics, MicrometerHermesMetrics micrometerHermesMetrics,
                       KafkaTopics kafkaTopics, boolean blacklisted) {
        this.topic = topic;
        this.kafkaTopics = kafkaTopics;
        this.hermesMetrics = oldHermesMetrics;
        this.blacklisted = blacklisted;

        globalRequestMeter = oldHermesMetrics.meter(Meters.METER);
        topicRequestMeter = oldHermesMetrics.meter(Meters.TOPIC_METER, topic.getName());

        globalDelayedProcessingMeter = oldHermesMetrics.meter(Meters.DELAYED_PROCESSING);
        topicDelayedProcessingMeter = oldHermesMetrics.meter(Meters.TOPIC_DELAYED_PROCESSING, topic.getName());

        topicMessageContentSize = oldHermesMetrics.messageContentSizeHistogram(topic.getName());
        globalMessageContentSize = oldHermesMetrics.messageContentSizeHistogram();

        published = oldHermesMetrics.counter(Counters.PUBLISHED, topic.getName());

        globalThroughputMeter = oldHermesMetrics.meter(Meters.THROUGHPUT_BYTES);
        topicThroughputMeter = oldHermesMetrics.meter(Meters.TOPIC_THROUGHPUT_BYTES, topic.getName());

        if (Topic.Ack.ALL.equals(topic.getAck())) {
            globalProducerLatencyTimer = HermesTimer.from(
                    micrometerHermesMetrics.timer("ack-all.global-latency"),
                    oldHermesMetrics.timer(Timers.ACK_ALL_LATENCY));
            topicProducerLatencyTimer = HermesTimer.from(
                    micrometerHermesMetrics.timer("ack-all.topic-latency", topic.getName()),
                    oldHermesMetrics.timer(Timers.ACK_ALL_TOPIC_LATENCY, topic.getName()));
            topicBrokerLatencyTimer = HermesTimer.from(
                    micrometerHermesMetrics.timer("ack-all.broker-latency"),
                    oldHermesMetrics.timer(Timers.ACK_ALL_BROKER_LATENCY));
        } else {
            globalProducerLatencyTimer = HermesTimer.from(
                    micrometerHermesMetrics.timer("ack-leader.global-latency"),
                    oldHermesMetrics.timer(Timers.ACK_LEADER_LATENCY));
            topicProducerLatencyTimer = HermesTimer.from(
                    micrometerHermesMetrics.timer("ack-leader.topic-latency", topic.getName()),
                    oldHermesMetrics.timer(Timers.ACK_LEADER_TOPIC_LATENCY, topic.getName()));
            topicBrokerLatencyTimer = HermesTimer.from(
                    micrometerHermesMetrics.timer("ack-leader.broker-latency"),
                    oldHermesMetrics.timer(Timers.ACK_LEADER_BROKER_LATENCY));
        }
    }

    public Topic getTopic() {
        return topic;
    }

    public TopicName getTopicName() {
        return topic.getName();
    }

    public String getQualifiedName() {
        return topic.getName().qualifiedName();
    }

    public KafkaTopics getKafkaTopics() {
        return kafkaTopics;
    }

    public boolean isBlacklisted() {
        return blacklisted;
    }

    public StartedTimersPair startProducerLatencyTimers() {
        return new StartedTimersPair(topicProducerLatencyTimer.time(), globalProducerLatencyTimer.time());
    }

    public void markStatusCodeMeter(int status) {
        httpStatusCodesMeters.computeIfAbsent(
                status,
                code -> new MetersPair(
                    hermesMetrics.httpStatusCodeMeter(status),
                    hermesMetrics.httpStatusCodeMeter(status, topic.getName()))
        ).mark();
    }

    public void markRequestMeter() {
        globalRequestMeter.mark();
        topicRequestMeter.mark();
    }

    public HermesTimerContext startBrokerLatencyTimer() {
        return topicBrokerLatencyTimer.time();
    }

    public void incrementPublished() {
        published.inc();
    }

    public void reportMessageContentSize(int size) {
        topicMessageContentSize.update(size);
        globalMessageContentSize.update(size);
        topicThroughputMeter.mark(size);
        globalThroughputMeter.mark(size);
    }

    public void markDelayedProcessing() {
        topicDelayedProcessingMeter.mark();
        globalDelayedProcessingMeter.mark();
    }

    public Metered getThroughput() {
        return topicThroughputMeter;
    }
}
