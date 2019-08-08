package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints

import pl.allegro.tech.hermes.api.SubscriptionName
import spock.lang.Specification
import spock.lang.Unroll

class WorkloadConstraintsTest extends Specification {

    static DEFAULT_CONSUMERS_PER_SUBSCRIPTION = 2
    static DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER = 2

    static sub1 = new SubscriptionConstraints(SubscriptionName.fromString('group.topic$sub1'), 3)
    static sub2 = new SubscriptionConstraints(SubscriptionName.fromString('group.topic$sub2'), 1)
    static undefinedSubscription = SubscriptionName.fromString('group.topic$undefined')

    @Unroll
    def "should return constraints of given subscription or default value if constraints don't exist"() {
        given:
        def workloadConstraints = new WorkloadConstraints(
                [(sub1.subscriptionName): sub1, (sub2.subscriptionName): sub2],
                DEFAULT_CONSUMERS_PER_SUBSCRIPTION,
                DEFAULT_MAX_SUBSCRIPTIONS_PER_CONSUMER
        )

        expect:
        workloadConstraints.getSubscriptionConstraints(subscriptionName).requiredConsumersNumber == expectedResult

        where:
        subscriptionName        | expectedResult
        sub1.subscriptionName   | sub1.requiredConsumersNumber
        undefinedSubscription   | DEFAULT_CONSUMERS_PER_SUBSCRIPTION
    }
}
