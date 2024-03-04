package pl.allegro.tech.hermes.frontend.server

import com.google.common.collect.ImmutableList
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache
import pl.allegro.tech.hermes.frontend.metric.CachedTopic
import pl.allegro.tech.hermes.frontend.producer.BrokerTopicMetadataFetcher
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
        BrokerTopicMetadataFetcher checker = Mock()
        def hook = new TopicMetadataLoadingRunner(checker, "dc1", ["dc1", "dc2"], topicsCache, 2, Duration.ofSeconds(10), 2)

        when:
        hook.refreshMetadataForLocalDatacenter()

        then:
        for (String topic : topics) {
            1 * checker.tryFetchFromDatacenter(cachedTopics.get(topic), "dc1") >> true
        }
    }

    def "should retry loading topic metadata"() {
        given:
        BrokerTopicMetadataFetcher checker = Mock()
        def hook = new TopicMetadataLoadingRunner(checker, "dc1", ["dc1", "dc2"], topicsCache, 2, Duration.ofSeconds(10), 4)

        when:
        hook.refreshMetadataForLocalDatacenter()

        then:
        1 * checker.tryFetchFromDatacenter(cachedTopics.get("g1.topicA"), "dc1") >> false
        1 * checker.tryFetchFromDatacenter(cachedTopics.get("g1.topicA"), "dc1") >> true

        1 * checker.tryFetchFromDatacenter(cachedTopics.get("g1.topicB"), "dc1") >> true

        2 * checker.tryFetchFromDatacenter(cachedTopics.get("g2.topicC"), "dc1") >> false
        1 * checker.tryFetchFromDatacenter(cachedTopics.get("g2.topicC"), "dc1") >> true
    }

    def "should leave retry loop when reached max retries and failed to load metadata"() {
        given:
        BrokerTopicMetadataFetcher checker = Mock()
        def hook = new TopicMetadataLoadingRunner(checker, "dc1", ["dc1", "dc2"], topicsCache, 2, Duration.ofSeconds(10), 4)

        when:
        hook.refreshMetadataForLocalDatacenter()

        then:
        3 * checker.tryFetchFromDatacenter(cachedTopics.get("g1.topicA"), "dc1") >> false
        1 * checker.tryFetchFromDatacenter(cachedTopics.get("g1.topicB"), "dc1") >> true
        1 * checker.tryFetchFromDatacenter(cachedTopics.get("g2.topicC"), "dc1") >> true
    }

    def "should not throw exception when no topics exist"() {
        given:
        BrokerTopicMetadataFetcher checker = Mock()
        TopicsCache emptyCache = Mock() {
            getTopics() >> []
        }
        def hook = new TopicMetadataLoadingRunner(checker, "dc1", ["dc1", "dc2"], emptyCache, 2, Duration.ofSeconds(10), 4)

        when:
        hook.refreshMetadataForLocalDatacenter()

        then:
        noExceptionThrown()
    }
}
