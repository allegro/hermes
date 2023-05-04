package pl.allegro.tech.hermes.frontend.server

import com.codahale.metrics.MetricRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.common.kafka.KafkaTopic
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName
import pl.allegro.tech.hermes.common.kafka.KafkaTopics
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.common.metric.micrometer.MicrometerHermesMetrics
import pl.allegro.tech.hermes.frontend.metric.CachedTopic
import pl.allegro.tech.hermes.metrics.MetricRegistryPathsCompiler
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder

class CachedTopicsTestHelper {

    static HermesMetrics hermesMetrics = new HermesMetrics(
            new MetricRegistry(), new MetricRegistryPathsCompiler("localhost"))

    static MicrometerHermesMetrics micrometerHermesMetrics = new MicrometerHermesMetrics(new SimpleMeterRegistry())

    static CachedTopic cachedTopic(String name) {
        def kafkaTopics = new KafkaTopics(new KafkaTopic(KafkaTopicName.valueOf(name), ContentType.JSON))
        return new CachedTopic(TopicBuilder.topic(name).build(), hermesMetrics, micrometerHermesMetrics, kafkaTopics)
    }

    static CachedTopic cachedTopic(Topic topic) {
        def kafkaTopics = new KafkaTopics(new KafkaTopic(KafkaTopicName.valueOf(topic.qualifiedName), ContentType.JSON))
        return new CachedTopic(topic, hermesMetrics, micrometerHermesMetrics, kafkaTopics)
    }
}
