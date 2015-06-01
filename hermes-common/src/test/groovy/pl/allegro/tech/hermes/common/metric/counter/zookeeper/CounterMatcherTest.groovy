package pl.allegro.tech.hermes.common.metric.counter.zookeeper

import spock.lang.Specification

class CounterMatcherTest extends Specification {

    def "should match topic published"() {
        given:
        def counterName = "producer.localhost_domain.published.lagMetricGroup.topic"
        def counterMatcher = new CounterMatcher(counterName, "localhost.domain")

        when:
        def isTopic = counterMatcher.isTopicPublished()
        def topicName = counterMatcher.topicName

        then:
        isTopic
        topicName == "lagMetricGroup.topic"
    }

    def "should match subscription delivered"() {
        given:
        def counterName = "consumer.localhost_domain.delivered.lagMetricGroup.topic.subscription"
        def counterMatcher = new CounterMatcher(counterName, "localhost.domain")

        when:
        def isSubscription = counterMatcher.isSubscriptionDelivered()
        def topicName = counterMatcher.topicName
        def subscriptionName = counterMatcher.subscriptionName

        then:
        isSubscription
        topicName == "lagMetricGroup.topic"
        subscriptionName == "subscription"
    }

    def "should match subscription discarded"() {
        given:
        def counterName = "consumer.localhost_domain.discarded.lagMetricGroup.topic.subscription"
        def counterMatcher = new CounterMatcher(counterName, "localhost.domain")

        when:
        def isSubscription = counterMatcher.isSubscriptionDiscarded()
        def topicName = counterMatcher.topicName
        def subscriptionName = counterMatcher.subscriptionName

        then:
        isSubscription
        topicName == "lagMetricGroup.topic"
        subscriptionName == "subscription"
    }

    def "should match inflight counter"() {
        given:
        def counterName = "consumer.localhost_domain.inflight.group.topic.subscription"
        def counterMatcher = new CounterMatcher(counterName, "localhost.domain")

        when:
        def isInflight = counterMatcher.isSubscriptionInflight()
        def topicName = counterMatcher.topicName
        def subscriptionName = counterMatcher.subscriptionName

        then:
        isInflight
        topicName == "group.topic"
        subscriptionName == "subscription"
    }
}