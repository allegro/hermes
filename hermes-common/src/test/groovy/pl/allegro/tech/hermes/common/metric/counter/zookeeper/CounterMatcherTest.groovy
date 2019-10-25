package pl.allegro.tech.hermes.common.metric.counter.zookeeper

import spock.lang.Specification

class CounterMatcherTest extends Specification {

    def "should match topic published"() {
        given:
        def counterName = "published.lagMetricGroup.topic"
        def counterMatcher = new CounterMatcher(counterName)

        when:
        def isTopic = counterMatcher.isTopicPublished()
        def topicName = counterMatcher.topicName

        then:
        isTopic
        topicName == "lagMetricGroup.topic"
    }

    def "should match subscription delivered"() {
        given:
        def counterName = "delivered.lagMetricGroup.topic.subscription"
        def counterMatcher = new CounterMatcher(counterName)

        when:
        def isSubscription = counterMatcher.isSubscriptionDelivered()
        def topicName = counterMatcher.topicName
        def subscriptionName = counterMatcher.subscriptionName

        then:
        isSubscription
        topicName == "lagMetricGroup.topic"
        subscriptionName == "subscription"
    }

    def "should match subscription delivered with underscores in name"() {
        given:
        def counterName = "delivered.pl_allegro_offercore.topic.auction_offer-ended-only"
        def counterMatcher = new CounterMatcher(counterName)

        when:
        def isSubscription = counterMatcher.isSubscriptionDelivered()
        def topicName = counterMatcher.topicName
        def subscriptionName = counterMatcher.subscriptionName

        then:
        isSubscription
        topicName == "pl_allegro_offercore.topic"
        subscriptionName == "auction_offer-ended-only"
    }

    def "should match subscription discarded"() {
        given:
        def counterName = "discarded.lagMetricGroup.topic.subscription"
        def counterMatcher = new CounterMatcher(counterName)

        when:
        def isSubscription = counterMatcher.isSubscriptionDiscarded()
        def topicName = counterMatcher.topicName
        def subscriptionName = counterMatcher.subscriptionName

        then:
        isSubscription
        topicName == "lagMetricGroup.topic"
        subscriptionName == "subscription"
    }
}