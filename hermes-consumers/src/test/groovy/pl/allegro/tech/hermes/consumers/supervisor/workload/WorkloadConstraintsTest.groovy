package pl.allegro.tech.hermes.consumers.supervisor.workload

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
        def workloadConstraints = WorkloadConstraints.builder()
                .withActiveConsumers(AVAILABLE_CONSUMERS)
                .withConsumersPerSubscription(DEFAULT_CONSUMERS_PER_SUBSCRIPTION)
                .withMaxSubscriptionsPerConsumer(DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER)
                .withSubscriptionConstraints([
                        (SubscriptionName.fromString('group.topic$sub1')): new Constraints(3, null),
                        (SubscriptionName.fromString('group.topic$sub2')): new Constraints(1, null)
                ])
                .build()

        expect:
        workloadConstraints.getConsumerCount(subscriptionName) == expectedResult

        where:
        subscriptionName                                        | expectedResult
        SubscriptionName.fromString('group.topic$sub1')         | 3
        SubscriptionName.fromString('group.topic$undefined')    | DEFAULT_CONSUMERS_PER_SUBSCRIPTION
    }

    @Unroll
    def "should return subscription constraints or topic constraints if given subscription has no constraints (#subscriptionName)"() {
        def workloadConstraints = WorkloadConstraints.builder()
                .withActiveConsumers(AVAILABLE_CONSUMERS)
                .withConsumersPerSubscription(DEFAULT_CONSUMERS_PER_SUBSCRIPTION)
                .withMaxSubscriptionsPerConsumer(DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER)
                .withSubscriptionConstraints([
                        (SubscriptionName.fromString('group.topic$sub1')): new Constraints(3, null)
                ])
                .withTopicConstraints([
                        (TopicName.fromQualifiedName('group.topic')): new Constraints(2, null)
                ])
                .build()

        expect:
        workloadConstraints.getConsumerCount(subscriptionName) == expectedResult

        where:
        subscriptionName                                | expectedResult
        SubscriptionName.fromString('group.topic$sub1') | 3
        SubscriptionName.fromString('group.topic$sub2') | 2
    }

    @Unroll
    def "should return max available number of consumers if specified constraint is higher than available consumers number"() {
        given:
        def workloadConstraints = WorkloadConstraints.builder()
                .withActiveConsumers(AVAILABLE_CONSUMERS)
                .withConsumersPerSubscription(DEFAULT_CONSUMERS_PER_SUBSCRIPTION)
                .withMaxSubscriptionsPerConsumer(DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER)
                .withSubscriptionConstraints([
                        (SubscriptionName.fromString('group.topic1$sub')): new Constraints(AVAILABLE_CONSUMERS + 1, null)
                ])
                .withTopicConstraints([
                        (TopicName.fromQualifiedName('group.topic2')): new Constraints(AVAILABLE_CONSUMERS + 1, null)
                ])
                .build()

        expect:
        workloadConstraints.getConsumerCount(subscriptionName) == AVAILABLE_CONSUMERS

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
        def workloadConstraints = WorkloadConstraints.builder()
                .withActiveConsumers(AVAILABLE_CONSUMERS)
                .withConsumersPerSubscription(DEFAULT_CONSUMERS_PER_SUBSCRIPTION)
                .withMaxSubscriptionsPerConsumer(DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER)
                .withTopicConstraints([
                        (TopicName.fromQualifiedName('group.topic')): new Constraints(incorrectConsumersNumber, null)
                ])
                .build()

        expect:
        workloadConstraints.getConsumerCount(subscriptionName) == DEFAULT_CONSUMERS_PER_SUBSCRIPTION

        where:
        incorrectConsumersNumber << [0, -1]
    }

    @Unroll
    def "should return default number of consumers if specified constraints for subscription have value less or equal to 0 (#incorrectConsumersNumber)"() {
        given:
        def subscriptionName = SubscriptionName.fromString('group.incorrect_topic$sub')
        def workloadConstraints = WorkloadConstraints.builder()
                .withActiveConsumers(AVAILABLE_CONSUMERS)
                .withConsumersPerSubscription(DEFAULT_CONSUMERS_PER_SUBSCRIPTION)
                .withMaxSubscriptionsPerConsumer(DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER)
                .withSubscriptionConstraints([
                        (subscriptionName): new Constraints(incorrectConsumersNumber, null)
                ])
                .build()

        expect:
        workloadConstraints.getConsumerCount(subscriptionName) == DEFAULT_CONSUMERS_PER_SUBSCRIPTION

        where:
        incorrectConsumersNumber << [0, -1]
    }

    @Unroll
    def "should return default number of consumers if specified constraints are null"() {
        given:
        def subscriptionName = SubscriptionName.fromString('group.incorrect_topic$sub')
        def workloadConstraints = WorkloadConstraints.builder()
                .withActiveConsumers(AVAILABLE_CONSUMERS)
                .withConsumersPerSubscription(DEFAULT_CONSUMERS_PER_SUBSCRIPTION)
                .withMaxSubscriptionsPerConsumer(DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER)
                .withSubscriptionConstraints(constraintsSubscription as Map )
                .withTopicConstraints(constraintsTopic as Map)
                .build()

        expect:
        workloadConstraints.getConsumerCount(subscriptionName) == DEFAULT_CONSUMERS_PER_SUBSCRIPTION

        where:
        constraintsSubscription | constraintsTopic
        null                    | emptyMap()
        emptyMap()              | null
    }
}
