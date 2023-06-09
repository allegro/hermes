package pl.allegro.tech.hermes.frontend.metric;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.metric.timer.StartedTimersPair;
import pl.allegro.tech.hermes.common.metric.HermesCounter;
import pl.allegro.tech.hermes.common.metric.HermesRateMeter;
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

    private final HermesCounter topicThroughputMeter;
    private final HermesCounter globalThroughputMeter;

    private final Counter published;

    private final Map<Integer, MetersPair> httpStatusCodesMeters = new ConcurrentHashMap<>();

    public CachedTopic(Topic topic, HermesMetrics hermesMetrics, MetricsFacade metricsFacade,
                       KafkaTopics kafkaTopics) {
        this(topic, hermesMetrics, metricsFacade, kafkaTopics, false);
    }

    public CachedTopic(Topic topic, HermesMetrics oldHermesMetrics, MetricsFacade metricsFacade,
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

        globalThroughputMeter = metricsFacade.topics().topicGlobalThroughputBytes();
        topicThroughputMeter = metricsFacade.topics().topicThroughputBytes(topic.getName());

        if (Topic.Ack.ALL.equals(topic.getAck())) {
            globalProducerLatencyTimer = metricsFacade.topics().ackAllGlobalLatency();
            topicProducerLatencyTimer = metricsFacade.topics().ackAllTopicLatency(topic.getName());
            topicBrokerLatencyTimer = metricsFacade.topics().ackAllBrokerLatency();
        } else {
            globalProducerLatencyTimer = metricsFacade.topics().ackLeaderGlobalLatency();
            topicProducerLatencyTimer = metricsFacade.topics().ackLeaderTopicLatency(topic.getName());
            topicBrokerLatencyTimer = metricsFacade.topics().ackLeaderBrokerLatency();
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
        topicThroughputMeter.increment(size);
        globalThroughputMeter.increment(size);
    }

    public void markDelayedProcessing() {
        topicDelayedProcessingMeter.mark();
        globalDelayedProcessingMeter.mark();
    }

    public HermesRateMeter getThroughput() {
        return topicThroughputMeter;
    }
}
