package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import spock.lang.Specification
import spock.lang.Unroll

class WorkloadConstraintsTest extends Specification {

    static DEFAULT_CONSUMERS_PER_SUBSCRIPTION = 2
    static DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER = 2
    static AVAILABLE_CONSUMERS = 4

    static sub1 = new SubscriptionConstraints(SubscriptionName.fromString('group.topic$sub1'), 3)
    static sub2 = new SubscriptionConstraints(SubscriptionName.fromString('group.topic$sub2'), 1)
    static undefinedSubscription = SubscriptionName.fromString('group.topic$undefined')
    static topicConstraints = new TopicConstraints(TopicName.fromQualifiedName('group.topic_constraints'), 3)
    static topicSubscription = SubscriptionName.fromString('group.topic_constraints$sub')

    static incorrectSubscriptionConstraints = new SubscriptionConstraints(SubscriptionName.fromString('group.topic$sub3'), AVAILABLE_CONSUMERS + 1)
    static incorrectTopicConstraints = new TopicConstraints(TopicName.fromQualifiedName('group.topic_constraints'), AVAILABLE_CONSUMERS + 1)

    @Unroll
    def "should return constraints of given subscription or default value if constraints don't exist"() {
        given:
        def workloadConstraints = new WorkloadConstraints(
                [sub1, sub2], [topicConstraints],
                DEFAULT_CONSUMERS_PER_SUBSCRIPTION,
                DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER,
                AVAILABLE_CONSUMERS
        )

        expect:
        workloadConstraints.getConsumersNumber(subscriptionName) == expectedResult

        where:
        subscriptionName        | expectedResult
        sub1.subscriptionName   | sub1.consumersNumber
        topicSubscription       | topicConstraints.consumersNumber
        undefinedSubscription   | DEFAULT_CONSUMERS_PER_SUBSCRIPTION
    }

    @Unroll
    def "should return default number of consumers if specified constraint is higher than available consumers number"() {
        given:
        def workloadConstraints = new WorkloadConstraints(
                [incorrectSubscriptionConstraints], [incorrectTopicConstraints],
                DEFAULT_CONSUMERS_PER_SUBSCRIPTION,
                DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER,
                AVAILABLE_CONSUMERS
        )

        expect:
        workloadConstraints.getConsumersNumber(subscriptionName as SubscriptionName) == DEFAULT_CONSUMERS_PER_SUBSCRIPTION

        where:
        subscriptionName << [incorrectSubscriptionConstraints.subscriptionName, topicSubscription]
    }

    @Unroll
    def "should return default number of consumers if specified constraints for topic have value less or equal to 0 (#incorrectConsumersNumber)"() {
        given:
        def subscriptionName = SubscriptionName.fromString('group.incorrect_topic$sub')
        def workloadConstraints = new WorkloadConstraints(
                [],
                [new TopicConstraints(TopicName.fromQualifiedName('group.incorrect_topic'), incorrectConsumersNumber)],
                DEFAULT_CONSUMERS_PER_SUBSCRIPTION,
                DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER,
                AVAILABLE_CONSUMERS
        )

        expect:
        workloadConstraints.getConsumersNumber(subscriptionName) == DEFAULT_CONSUMERS_PER_SUBSCRIPTION

        where:
        incorrectConsumersNumber << [0, -1]
    }

    @Unroll
    def "should return default number of consumers if specified constraints for subscription have value less or equal to 0 (#incorrectConsumersNumber)"() {
        given:
        def subscriptionName = SubscriptionName.fromString('group.incorrect_topic$sub')
        def workloadConstraints = new WorkloadConstraints(
                [new SubscriptionConstraints(subscriptionName, incorrectConsumersNumber)],
                [],
                DEFAULT_CONSUMERS_PER_SUBSCRIPTION,
                DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER,
                AVAILABLE_CONSUMERS
        )

        expect:
        workloadConstraints.getConsumersNumber(subscriptionName) == DEFAULT_CONSUMERS_PER_SUBSCRIPTION

        where:
        incorrectConsumersNumber << [0, -1]
    }
}
