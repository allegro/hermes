package pl.allegro.tech.hermes.benchmark.environment;

import com.codahale.metrics.MetricRegistry;
import java.util.List;
import java.util.Optional;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.metric.ThroughputRegistry;

class InMemoryTopicsCache implements TopicsCache {

  private final MetricsFacade metricsFacade;
  private final KafkaTopics kafkaTopics;
  private final Topic topic;
  private final ThroughputRegistry throughputRegistry;

  InMemoryTopicsCache(MetricsFacade metricsFacade, Topic topic) {
    this.metricsFacade = metricsFacade;
    this.topic = topic;
    this.kafkaTopics =
        new KafkaTopics(
            new KafkaTopic(
                KafkaTopicName.valueOf(topic.getQualifiedName()), topic.getContentType()));
    this.throughputRegistry = new ThroughputRegistry(metricsFacade, new MetricRegistry());
  }

  @Override
  public Optional<CachedTopic> getTopic(String qualifiedTopicName) {
    if (qualifiedTopicName.equals(topic.getQualifiedName())) {
      return Optional.of(new CachedTopic(topic, metricsFacade, throughputRegistry, kafkaTopics));
    }
    return Optional.empty();
  }

  @Override
  public List<CachedTopic> getTopics() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void start() {
    throw new UnsupportedOperationException();
  }
}
