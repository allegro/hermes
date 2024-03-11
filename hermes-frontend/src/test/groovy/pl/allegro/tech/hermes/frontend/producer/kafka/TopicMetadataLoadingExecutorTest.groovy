package pl.allegro.tech.hermes.frontend.producer.kafka

import com.google.common.collect.ImmutableList
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache
import pl.allegro.tech.hermes.frontend.metric.CachedTopic
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration

import static pl.allegro.tech.hermes.frontend.producer.kafka.TopicMetadataLoader.*
import static pl.allegro.tech.hermes.frontend.producer.kafka.TopicMetadataLoader.MetadataLoadingResult.*
import static pl.allegro.tech.hermes.frontend.server.CachedTopicsTestHelper.cachedTopic

class TopicMetadataLoadingExecutorTest extends Specification {

    @Shared
    List<String> topics = ["g1.topicA", "g1.topicB", "g2.topicC"]

    @Shared
    Map<String, CachedTopic> cachedTopics = new HashMap<>()

    @Shared
    TopicsCache topicsCache

    def setupSpec() {
        for (String topic : topics) {
            cachedTopics.put(topic, cachedTopic(topic))
        }
        topicsCache = Mock() {
            getTopics() >> ImmutableList.copyOf(cachedTopics.values())
        }
    }

    def "should load topic metadata"() {
        given:
        TopicMetadataLoader loader = Mock()
        def executor = new TopicMetadataLoadingExecutor(topicsCache, 2, Duration.ofSeconds(10), 2)

        when:
        executor.execute([loader])

        then:
        for (String topic : topics) {
            1 * loader.load(cachedTopics.get(topic)) >> success(topic)
        }
    }

    def "should retry loading topic metadata"() {
        given:
        TopicMetadataLoader loader = Mock()
        def executor = new TopicMetadataLoadingExecutor(topicsCache, 2, Duration.ofSeconds(10), 4)

        when:
        executor.execute([loader])

        then:
        1 * loader.load(cachedTopics.get("g1.topicA")) >> failure("g1.topicA")
        1 * loader.load(cachedTopics.get("g1.topicA")) >> success("g1.topicA")

        1 * loader.load(cachedTopics.get("g1.topicB")) >> success("g1.topicB")

        2 * loader.load(cachedTopics.get("g2.topicC")) >> failure("g2.topicC")
        1 * loader.load(cachedTopics.get("g2.topicC")) >> success("g2.topicC")
    }

    def "should leave retry loop when reached max retries and failed to load metadata"() {
        given:
        TopicMetadataLoader loader = Mock()
        def executor = new TopicMetadataLoadingExecutor(topicsCache, 2, Duration.ofSeconds(10), 4)

        when:
        executor.execute([loader])

        then:
        3 * loader.load(cachedTopics.get("g1.topicA")) >> failure("g1.topicA")
        1 * loader.load(cachedTopics.get("g1.topicB")) >> success("g1.topicB")
        1 * loader.load(cachedTopics.get("g2.topicC")) >> success("g2.topicC")
    }

    def "should not throw exception when no topics exist"() {
        given:
        TopicMetadataLoader loader = Mock()
        TopicsCache emptyCache = Mock() {
            getTopics() >> []
        }
        def executor = new TopicMetadataLoadingExecutor(emptyCache, 2, Duration.ofSeconds(10), 4)

        when:
        executor.execute([loader])

        then:
        noExceptionThrown()
    }

    private MetadataLoadingResult success(String topicName) {
        return success(cachedTopics.get(topicName).topic.name, "dc1")
    }

    private MetadataLoadingResult failure(String topicName) {
        return failure(cachedTopics.get(topicName).topic.name, "dc1")
    }
}
