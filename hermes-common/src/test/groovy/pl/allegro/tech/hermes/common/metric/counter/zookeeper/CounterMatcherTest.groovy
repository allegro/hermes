package pl.allegro.tech.hermes.common.metric.counter.zookeeper

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import pl.allegro.tech.hermes.api.TopicName
import spock.lang.Shared
import spock.lang.Specification

class CounterMatcherTest extends Specification {

    @Shared
    def meterRegistry = new SimpleMeterRegistry()

    def "should match topic published"() {
        given:
        def counter = meterRegistry.counter("topic.published", "group", "lagMetricGroup", "topic", "topic")
        def counterMatcher = new CounterMatcher(counter)

        when:
        def isTopic = counterMatcher.isTopicPublished()
        def topicName = counterMatcher.topicName

        then:
        isTopic
        topicName == new TopicName("lagMetricGroup", "topic")
    }

    def "should match subscription delivered"() {
        given:
        def counter = meterRegistry.counter("subscription.delivered", "group", "lagMetricGroup",
                "topic", "topic", "subscription", "subscription")
        def counterMatcher = new CounterMatcher(counter)

        when:
        def isSubscription = counterMatcher.isSubscriptionDelivered()
        def topicName = counterMatcher.topicName
        def subscriptionName = counterMatcher.subscriptionName

        then:
        isSubscription
        topicName == new TopicName("lagMetricGroup", "topic")
        subscriptionName == "subscription"
    }

    def "should match subscription discarded"() {
        given:
        def counter = meterRegistry.counter("subscription.discarded", "group", "lagMetricGroup",
                "topic", "topic", "subscription", "subscription")
        def counterMatcher = new CounterMatcher(counter)

        when:
        def isSubscription = counterMatcher.isSubscriptionDiscarded()
        def topicName = counterMatcher.topicName
        def subscriptionName = counterMatcher.subscriptionName

        then:
        isSubscription
        topicName == new TopicName("lagMetricGroup", "topic")
        subscriptionName == "subscription"
    }
}