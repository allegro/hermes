package pl.allegro.tech.hermes.domain.workload.constraints

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.api.Constraints
import spock.lang.Specification
import spock.lang.Unroll

import static java.util.Collections.emptyMap

class WorkloadConstraintsTest extends Specification {

    static DEFAULT_CONSUMERS_PER_SUBSCRIPTION = 2
    static DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER = 2
    static AVAILABLE_CONSUMERS = 4

    @Unroll
    def "should return constraints of given subscription or default value if constraints don't exist (#subscriptionName)"() {
        given:
        def workloadConstraints = new WorkloadConstraints(
                [(SubscriptionName.fromString('group.topic$sub1')): new Constraints(3),
                 (SubscriptionName.fromString('group.topic$sub2')): new Constraints(1)],
                emptyMap(),
                DEFAULT_CONSUMERS_PER_SUBSCRIPTION,
                DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER,
                AVAILABLE_CONSUMERS
        )

        expect:
        workloadConstraints.getConsumersNumber(subscriptionName) == expectedResult

        where:
        subscriptionName                                        | expectedResult
        SubscriptionName.fromString('group.topic$sub1')         | 3
        SubscriptionName.fromString('group.topic$undefined')    | DEFAULT_CONSUMERS_PER_SUBSCRIPTION
    }

    @Unroll
    def "should return subscription constraints or topic constraints if given subscription has no constraints (#subscriptionName)"() {
        def workloadConstraints = new WorkloadConstraints(
                [(SubscriptionName.fromString('group.topic$sub1')): new Constraints(3)],
                [(TopicName.fromQualifiedName('group.topic')): new Constraints(2)],
                DEFAULT_CONSUMERS_PER_SUBSCRIPTION,
                DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER,
                AVAILABLE_CONSUMERS
        )

        expect:
        workloadConstraints.getConsumersNumber(subscriptionName) == expectedResult

        where:
        subscriptionName                                | expectedResult
        SubscriptionName.fromString('group.topic$sub1') | 3
        SubscriptionName.fromString('group.topic$sub2') | 2
    }

    @Unroll
    def "should return max available number of consumers if specified constraint is higher than available consumers number"() {
        given:
        def workloadConstraints = new WorkloadConstraints(
                [(SubscriptionName.fromString('group.topic1$sub')): new Constraints(AVAILABLE_CONSUMERS + 1)],
                [(TopicName.fromQualifiedName('group.topic2')): new Constraints(AVAILABLE_CONSUMERS + 1)],
                DEFAULT_CONSUMERS_PER_SUBSCRIPTION,
                DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER,
                AVAILABLE_CONSUMERS
        )

        expect:
        workloadConstraints.getConsumersNumber(subscriptionName) == AVAILABLE_CONSUMERS

        where:
        subscriptionName << [
                SubscriptionName.fromString('group.topic1$sub'),
                SubscriptionName.fromString('group.topic2$sub')
        ]
    }

    @Unroll
    def "should return default number of consumers if specified constraints for topic have value less or equal to 0 (#incorrectConsumersNumber)"() {
        given:
        def subscriptionName = SubscriptionName.fromString('group.topic$sub')
        def workloadConstraints = new WorkloadConstraints(
                emptyMap(),
                [(TopicName.fromQualifiedName('group.topic')): new Constraints(incorrectConsumersNumber)],
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
