package pl.allegro.tech.hermes.frontend.metric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.metric.timer.StartedTimersPair;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesHistogram;
import pl.allegro.tech.hermes.metrics.HermesRateMeter;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;

public class CachedTopic {

  private final Topic topic;
  private final KafkaTopics kafkaTopics;
  private final MetricsFacade metricsFacade;
  private final boolean blacklisted;

  private final HermesTimer topicProducerLatencyTimer;
  private final HermesTimer globalProducerLatencyTimer;

  private final HermesTimer topicBrokerLatencyTimer;

  private final HermesCounter globalRequestMeter;
  private final HermesCounter topicRequestMeter;

  private final HermesCounter globalDelayedProcessingMeter;
  private final HermesCounter topicDelayedProcessingMeter;

  private final HermesHistogram topicMessageContentSize;
  private final HermesHistogram globalMessageContentSize;

  private final ThroughputMeter throughputMeter;

  private final HermesCounter topicDuplicatedMessageCounter;

  private final Map<String, HermesCounter> published = new ConcurrentHashMap<>();

  private final Map<Integer, MetersPair> httpStatusCodesMeters = new ConcurrentHashMap<>();

  public CachedTopic(
      Topic topic,
      MetricsFacade metricsFacade,
      ThroughputRegistry throughputRegistry,
      KafkaTopics kafkaTopics) {
    this(topic, metricsFacade, throughputRegistry, kafkaTopics, false);
  }

  public CachedTopic(
      Topic topic,
      MetricsFacade metricsFacade,
      ThroughputRegistry throughputRegistry,
      KafkaTopics kafkaTopics,
      boolean blacklisted) {
    this.topic = topic;
    this.kafkaTopics = kafkaTopics;
    this.metricsFacade = metricsFacade;
    this.blacklisted = blacklisted;

    globalRequestMeter = metricsFacade.topics().topicGlobalRequestCounter();
    topicRequestMeter = metricsFacade.topics().topicRequestCounter(topic.getName());

    globalDelayedProcessingMeter = metricsFacade.topics().topicGlobalDelayedProcessingCounter();
    topicDelayedProcessingMeter =
        metricsFacade.topics().topicDelayedProcessingCounter(topic.getName());

    globalMessageContentSize = metricsFacade.topics().topicGlobalMessageContentSizeHistogram();
    topicMessageContentSize =
        metricsFacade.topics().topicMessageContentSizeHistogram(topic.getName());

    throughputMeter = throughputRegistry.forTopic(topic.getName());

    if (Topic.Ack.ALL.equals(topic.getAck())) {
      globalProducerLatencyTimer = metricsFacade.topics().ackAllGlobalLatency();
      topicProducerLatencyTimer = metricsFacade.topics().ackAllTopicLatency(topic.getName());
      topicBrokerLatencyTimer = metricsFacade.topics().ackAllBrokerLatency();
    } else {
      globalProducerLatencyTimer = metricsFacade.topics().ackLeaderGlobalLatency();
      topicProducerLatencyTimer = metricsFacade.topics().ackLeaderTopicLatency(topic.getName());
      topicBrokerLatencyTimer = metricsFacade.topics().ackLeaderBrokerLatency();
    }

    topicDuplicatedMessageCounter =
        metricsFacade.topics().topicDuplicatedMessageCounter(topic.getName());
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
    return new StartedTimersPair(
        topicProducerLatencyTimer.time(), globalProducerLatencyTimer.time());
  }

  public void markStatusCodeMeter(int status) {
    httpStatusCodesMeters
        .computeIfAbsent(
            status,
            code ->
                new MetersPair(
                    metricsFacade.topics().topicGlobalHttpStatusCodeCounter(status),
                    metricsFacade.topics().topicHttpStatusCodeCounter(topic.getName(), status)))
        .mark();
  }

  public void markRequestMeter() {
    globalRequestMeter.increment(1L);
    topicRequestMeter.increment(1L);
  }

  public HermesTimerContext startBrokerLatencyTimer() {
    return topicBrokerLatencyTimer.time();
  }

  public void incrementPublished(String datacenter) {
    published
        .computeIfAbsent(
            datacenter, dc -> metricsFacade.topics().topicPublished(topic.getName(), datacenter))
        .increment();
  }

  public void reportMessageContentSize(int size) {
    topicMessageContentSize.record(size);
    globalMessageContentSize.record(size);
    throughputMeter.increment(size);
  }

  public void markDelayedProcessing() {
    topicDelayedProcessingMeter.increment(1L);
    globalDelayedProcessingMeter.increment(1L);
  }

  public HermesRateMeter getThroughput() {
    return throughputMeter;
  }

  public void markMessageDuplicated() {
    topicDuplicatedMessageCounter.increment();
  }
}
