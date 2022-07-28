package pl.allegro.tech.hermes.frontend.server

import com.google.common.collect.ImmutableList
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache
import pl.allegro.tech.hermes.frontend.metric.CachedTopic
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration

import static pl.allegro.tech.hermes.frontend.server.CachedTopicsTestHelper.cachedTopic

class TopicMetadataLoadingRunnerTest extends Specification {

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
        BrokerMessageProducer producer = Mock()
        def hook = new TopicMetadataLoadingRunner(producer, topicsCache, 2, Duration.ofSeconds(10), 2)

        when:
        hook.refreshMetadata()

        then:
        for (String topic : topics) {
            1 * producer.isTopicAvailable(cachedTopics.get(topic)) >> true
        }
    }

    def "should retry loading topic metadata"() {
        given:
        BrokerMessageProducer producer = Mock()
        def hook = new TopicMetadataLoadingRunner(producer, topicsCache, 2, Duration.ofSeconds(10), 4)

        when:
        hook.refreshMetadata()

        then:
        1 * producer.isTopicAvailable(cachedTopics.get("g1.topicA")) >> false
        1 * producer.isTopicAvailable(cachedTopics.get("g1.topicA")) >> true

        1 * producer.isTopicAvailable(cachedTopics.get("g1.topicB")) >> true

        2 * producer.isTopicAvailable(cachedTopics.get("g2.topicC")) >> false
        1 * producer.isTopicAvailable(cachedTopics.get("g2.topicC")) >> true
    }

    def "should leave retry loop when reached max retries and failed to load metadata"() {
        given:
        BrokerMessageProducer producer = Mock()
        def hook = new TopicMetadataLoadingRunner(producer, topicsCache, 2, Duration.ofSeconds(10), 4)

        when:
        hook.refreshMetadata()

        then:
        3 * producer.isTopicAvailable(cachedTopics.get("g1.topicA")) >> false
        1 * producer.isTopicAvailable(cachedTopics.get("g1.topicB")) >> true
        1 * producer.isTopicAvailable(cachedTopics.get("g2.topicC")) >> true
    }

    def "should not throw exception when no topics exist"() {
        given:
        BrokerMessageProducer producer = Mock()
        TopicsCache emptyCache = Mock() {
            getTopics() >> []
        }
        def hook = new TopicMetadataLoadingRunner(producer, emptyCache, 2, Duration.ofSeconds(10), 4)

        when:
        hook.refreshMetadata()

        then:
        noExceptionThrown()
    }
}
