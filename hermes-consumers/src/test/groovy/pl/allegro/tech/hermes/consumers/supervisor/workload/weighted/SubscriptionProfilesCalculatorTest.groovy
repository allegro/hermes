package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkDistributionChanges
import pl.allegro.tech.hermes.test.helper.time.ModifiableClock
import spock.lang.Specification
import spock.lang.Subject

import java.time.Duration

import static pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.WeightedWorkloadAssertions.assertThat

class SubscriptionProfilesCalculatorTest extends Specification {

    def clock = new ModifiableClock()
    def workDistributionChanges = Mock(WorkDistributionChanges)
    def consumerNodeLoadRegistry = new MockConsumerNodeLoadRegistry()
    def subscriptionProfileRegistry = new MockSubscriptionProfileRegistry()
    def weightWindowSize = Duration.ofMinutes(1)

    @Subject
    def calculator = new SubscriptionProfilesCalculator(
            consumerNodeLoadRegistry,
            subscriptionProfileRegistry,
            clock,
            weightWindowSize
    )

    def cleanup() {
        consumerNodeLoadRegistry.reset()
        subscriptionProfileRegistry.reset()
        calculator.onBeforeBalancing([])
    }

    def "should take the maximal value of operations per second as the subscription weight"() {
        given:
        consumerNodeLoadRegistry
                .operationsPerSecond(subscription("sub1"), ["c1": 500d, "c2": 10d])
                .operationsPerSecond(subscription("sub2"), ["c1": 500d, "c2": 10d])
                .operationsPerSecond(subscription("sub3"), ["c3": 10d, "c4": 10d])

        when:
        calculator.onBeforeBalancing(["c1", "c2", "c3"])

        then:
        assertThat(calculator.get(subscription("sub1")))
                .hasWeight(new Weight(500d))
        assertThat(calculator.get(subscription("sub2")))
                .hasWeight(new Weight(500d))
        assertThat(calculator.get(subscription("sub3")))
                .hasWeight(new Weight(10d))
    }

    def "should calculate weight using exponentially weighted moving average"() {
        given:
        def previousRebalanceTimestamp = clock.instant()
        subscriptionProfileRegistry
                .updateTimestamp(previousRebalanceTimestamp)
                .profile(subscription("sub1"), previousRebalanceTimestamp, new Weight(100d))
        consumerNodeLoadRegistry
                .operationsPerSecond(subscription("sub1"), ["c1": 50d, "c2": 50d])

        when:
        clock.advance(weightWindowSize.minusSeconds(30))
        calculator.onBeforeBalancing(["c1", "c2"])

        then:
        assertThat(calculator.get(subscription("sub1")))
                .hasWeight(new Weight(80.32653298563167d))

        when:
        workDistributionChanges.getRebalancedSubscriptions() >> []
        calculator.onAfterBalancing(workDistributionChanges)
        clock.advance(weightWindowSize.minusSeconds(30))
        calculator.onBeforeBalancing(["c1", "c2"])

        then:
        assertThat(calculator.get(subscription("sub1")))
                .hasWeight(new Weight(68.39397205857212d))
    }

    def "should take 0 as the default weight"() {
        given:
        def previousRebalanceTimestamp = clock.instant()
        subscriptionProfileRegistry
                .updateTimestamp(previousRebalanceTimestamp)
                .profile(subscription("sub1"), previousRebalanceTimestamp, new Weight(100d))
                .profile(subscription("sub2"), previousRebalanceTimestamp, new Weight(20d))
        consumerNodeLoadRegistry
                .operationsPerSecond(subscription("sub1"), ["c1": 50d, "c2": 50d])

        when:
        clock.advance(weightWindowSize.minusSeconds(30))
        calculator.onBeforeBalancing(["c1", "c2"])

        then:
        assertThat(calculator.get(subscription("sub1")))
                .hasWeight(new Weight(80.32653298563167d))
        assertThat(calculator.get(subscription("sub2")))
                .hasWeight(Weight.ZERO)
        assertThat(calculator.get(subscription("sub3")))
                .hasWeight(Weight.ZERO)
    }

    def "should update rebalance timestamp"() {
        given:
        def previousRebalanceTimestamp = clock.instant()
        subscriptionProfileRegistry
                .profile(subscription("sub1"), previousRebalanceTimestamp, new Weight(50d))
                .profile(subscription("sub2"), previousRebalanceTimestamp, new Weight(20d))
        consumerNodeLoadRegistry
                .operationsPerSecond(subscription("sub1"), ["c1": 100d, "c2": 100d])
                .operationsPerSecond(subscription("sub2"), ["c1": 10d, "c2": 10d])
        workDistributionChanges.getRebalancedSubscriptions() >> [subscription("sub1")]

        and:
        clock.advanceMinutes(1)

        and:
        calculator.onBeforeBalancing(["c1", "c2", "c3"])

        when:
        calculator.onAfterBalancing(workDistributionChanges)

        then:
        assertThat(calculator.get(subscription("sub1")))
                .hasLastRebalanceTimestamp(clock.instant())
        assertThat(calculator.get(subscription("sub2")))
                .hasLastRebalanceTimestamp(previousRebalanceTimestamp)
    }

    private static SubscriptionName subscription(String name) {
        return SubscriptionName.fromString("pl.allegro.tech.hermes\$$name")
    }
}
