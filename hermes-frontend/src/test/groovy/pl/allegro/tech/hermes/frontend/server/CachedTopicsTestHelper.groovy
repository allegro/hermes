package pl.allegro.tech.hermes.frontend.server

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.common.kafka.KafkaTopic
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName
import pl.allegro.tech.hermes.common.kafka.KafkaTopics
import pl.allegro.tech.hermes.common.metric.MetricsFacade
import pl.allegro.tech.hermes.frontend.metric.CachedTopic
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder

class CachedTopicsTestHelper {

    static MetricsFacade micrometerHermesMetrics = new MetricsFacade(new SimpleMeterRegistry())

    static CachedTopic cachedTopic(String name) {
        def kafkaTopics = new KafkaTopics(new KafkaTopic(KafkaTopicName.valueOf(name), ContentType.JSON))
        return new CachedTopic(TopicBuilder.topic(name).build(), micrometerHermesMetrics, kafkaTopics)
    }

    static CachedTopic cachedTopic(Topic topic) {
        def kafkaTopics = new KafkaTopics(new KafkaTopic(KafkaTopicName.valueOf(topic.qualifiedName), ContentType.JSON))
        return new CachedTopic(topic, micrometerHermesMetrics, kafkaTopics)
    }
}
