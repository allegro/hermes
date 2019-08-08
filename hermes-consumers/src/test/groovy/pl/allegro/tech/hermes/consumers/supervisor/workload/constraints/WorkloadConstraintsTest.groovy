package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints

import pl.allegro.tech.hermes.api.SubscriptionName
import spock.lang.Specification
import spock.lang.Unroll

class WorkloadConstraintsTest extends Specification {

    static DEFAULT_CONSUMERS_PER_SUBSCRIPTION = 2
    static DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER = 2
    static AVAILABLE_CONSUMERS = 4

    static sub1 = new SubscriptionConstraints(SubscriptionName.fromString('group.topic$sub1'), 3)
    static sub2 = new SubscriptionConstraints(SubscriptionName.fromString('group.topic$sub2'), 1)
    static sub3 = new SubscriptionConstraints(SubscriptionName.fromString('group.topic$sub3'), AVAILABLE_CONSUMERS + 1)
    static undefinedSubscription = SubscriptionName.fromString('group.topic$undefined')

    @Unroll
    def "should return constraints of given subscription or default value if constraints don't exist"() {
        given:
        def workloadConstraints = new WorkloadConstraints(
                [(sub1.subscriptionName): sub1, (sub2.subscriptionName): sub2],
                DEFAULT_CONSUMERS_PER_SUBSCRIPTION,
                DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER,
                AVAILABLE_CONSUMERS
        )

        expect:
        workloadConstraints.getConsumersNumber(subscriptionName) == expectedResult

        where:
        subscriptionName        | expectedResult
        sub1.subscriptionName   | sub1.consumersNumber
        undefinedSubscription   | DEFAULT_CONSUMERS_PER_SUBSCRIPTION
    }

    def "should return default number of consumers if specified constraints is higher than available consumers"() {
        given:
        def workloadConstraints = new WorkloadConstraints(
                [(sub3.subscriptionName): sub3],
                DEFAULT_CONSUMERS_PER_SUBSCRIPTION,
                DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER,
                AVAILABLE_CONSUMERS
        )

        expect:
        workloadConstraints.getConsumersNumber(sub3.subscriptionName) == DEFAULT_CONSUMERS_PER_SUBSCRIPTION
    }
}
