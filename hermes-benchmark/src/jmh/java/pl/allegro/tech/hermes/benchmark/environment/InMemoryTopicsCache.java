package pl.allegro.tech.hermes.benchmark.environment;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

import java.util.List;
import java.util.Optional;

class InMemoryTopicsCache implements TopicsCache {

    private final HermesMetrics oldMetrics;
    private final MetricsFacade metricsFacade;
    private final KafkaTopics kafkaTopics;
    private final Topic topic;


    InMemoryTopicsCache(HermesMetrics oldMetrics, MetricsFacade metricsFacade, Topic topic) {
        this.oldMetrics = oldMetrics;
        this.metricsFacade = metricsFacade;
        this.topic = topic;
        this.kafkaTopics = new KafkaTopics(new KafkaTopic(KafkaTopicName.valueOf(topic.getQualifiedName()), topic.getContentType()));
    }

    @Override
    public Optional<CachedTopic> getTopic(String qualifiedTopicName) {
        if (qualifiedTopicName.equals(topic.getQualifiedName())) {
            return Optional.of(
                    new CachedTopic(
                            topic,
                            oldMetrics,
                            metricsFacade,
                            kafkaTopics
                    )
            );
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
