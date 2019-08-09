package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import spock.lang.Specification
import spock.lang.Unroll

import static java.util.Collections.emptyMap

class WorkloadConstraintsTest extends Specification {

    static DEFAULT_CONSUMERS_PER_SUBSCRIPTION = 2
    static DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER = 2
    static AVAILABLE_CONSUMERS = 4

    static sub1 = SubscriptionName.fromString('group.topic$sub1')
    static sub2 = SubscriptionName.fromString('group.topic$sub2')
    static undefinedSubscription = SubscriptionName.fromString('group.topic$undefined')
    static incorrectSubConstraints = SubscriptionName.fromString('group.topic$sub3')

    static topic = TopicName.fromQualifiedName('group.topic_constraints')
    static topicSubscription = SubscriptionName.fromString('group.topic_constraints$sub')

    @Unroll
    def "should return constraints of given subscription or default value if constraints don't exist"() {
        given:
        def workloadConstraints = new WorkloadConstraints(
                [(sub1): new Constraints(3), (sub2): new Constraints(1)],
                [(topic): new Constraints(3)],
                DEFAULT_CONSUMERS_PER_SUBSCRIPTION,
                DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER,
                AVAILABLE_CONSUMERS
        )

        expect:
        workloadConstraints.getConsumersNumber(subscriptionName) == expectedResult

        where:
        subscriptionName        | expectedResult
        sub1                    | 3
        topicSubscription       | 3
        undefinedSubscription   | DEFAULT_CONSUMERS_PER_SUBSCRIPTION
    }

    @Unroll
    def "should return default number of consumers if specified constraint is higher than available consumers number"() {
        given:
        def workloadConstraints = new WorkloadConstraints(
                [(incorrectSubConstraints): new Constraints(AVAILABLE_CONSUMERS + 1)],
                [(topic): new Constraints(AVAILABLE_CONSUMERS + 1)],
                DEFAULT_CONSUMERS_PER_SUBSCRIPTION,
                DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER,
                AVAILABLE_CONSUMERS
        )

        expect:
        workloadConstraints.getConsumersNumber(subscriptionName as SubscriptionName) == DEFAULT_CONSUMERS_PER_SUBSCRIPTION

        where:
        subscriptionName << [incorrectSubConstraints, topicSubscription]
    }

    @Unroll
    def "should return default number of consumers if specified constraints for topic have value less or equal to 0 (#incorrectConsumersNumber)"() {
        given:
        def subscriptionName = SubscriptionName.fromString('group.topic_constraints$sub')
        def workloadConstraints = new WorkloadConstraints(
                emptyMap(),
                [(topic): new Constraints(incorrectConsumersNumber)],
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
                [(subscriptionName): new Constraints(incorrectConsumersNumber)],
                emptyMap(),
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
    def "should return default number of consumers if specified constraints are null"() {
        given:
        def subscriptionName = SubscriptionName.fromString('group.incorrect_topic$sub')
        def workloadConstraints = new WorkloadConstraints(
                constraintsSubscription as Map,
                constraintsTopic as Map,
                DEFAULT_CONSUMERS_PER_SUBSCRIPTION,
                DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER,
                AVAILABLE_CONSUMERS
        )

        expect:
        workloadConstraints.getConsumersNumber(subscriptionName) == DEFAULT_CONSUMERS_PER_SUBSCRIPTION

        where:
        constraintsSubscription | constraintsTopic
        null                    | emptyMap()
        emptyMap()              | null
    }
}
