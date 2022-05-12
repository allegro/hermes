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

class InMemoryTopicsCache implements TopicsCache {

    private final HermesMetrics hermesMetrics;
    private final KafkaTopics kafkaTopics;
    private final Topic topic = topic(HermesServerEnvironment.BENCHMARK_TOPIC).withContentType(AVRO).build();


    InMemoryTopicsCache(HermesMetrics hermesMetrics) {
        this.hermesMetrics = hermesMetrics;

        Topic topic = topic(HermesServerEnvironment.BENCHMARK_TOPIC).withContentType(AVRO).build();
        this.kafkaTopics = new KafkaTopics(new KafkaTopic(KafkaTopicName.valueOf(topic.getQualifiedName()), topic.getContentType()));
    }

    @Override
    public Optional<CachedTopic> getTopic(String qualifiedTopicName) {
        return Optional.of(
                new CachedTopic(
                        topic,
                        hermesMetrics,
                        kafkaTopics
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
