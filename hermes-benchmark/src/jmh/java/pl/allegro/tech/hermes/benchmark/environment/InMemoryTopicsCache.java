package pl.allegro.tech.hermes.benchmark.environment;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopics;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

import java.util.List;
import java.util.Optional;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class InMemoryTopicsCache implements TopicsCache {

    private final HermesMetrics hermesMetrics;

    public InMemoryTopicsCache(HermesMetrics hermesMetrics) {
        this.hermesMetrics = hermesMetrics;
    }

    @Override
    public Optional<CachedTopic> getTopic(String qualifiedTopicName) {
        Topic topic = topic("bench.topic").withContentType(AVRO).build();
        return Optional.of(
                new CachedTopic(
                        topic,
                        hermesMetrics,
                        new KafkaTopics(new KafkaTopic(KafkaTopicName.valueOf(""), AVRO))
                )
        );
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
