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
        calculator.onBeforeBalancing([], [])
    }

    def "should take the maximal value of operations per second as the subscription weight"() {
        given:
        consumerNodeLoadRegistry
                .operationsPerSecond(subscription("sub1"), ["c1": 500d, "c2": 10d])
                .operationsPerSecond(subscription("sub2"), ["c1": 500d, "c2": 10d])
                .operationsPerSecond(subscription("sub3"), ["c3": 10d, "c4": 10d])

        when:
        calculator.onBeforeBalancing(["c1", "c2", "c3"], [subscription("sub1"), subscription("sub2"), subscription("sub3")])

        then:
        assertThat(calculator.get(subscription("sub1")))
                .hasWeight(new Weight(500d))
        assertThat(calculator.get(subscription("sub2")))
                .hasWeight(new Weight(500d))
        assertThat(calculator.get(subscription("sub3")))
                .hasWeight(new Weight(10d))
    }

    def "should combine previous weights with the current ones"() {
        given:
        def previousRebalanceTimestamp = clock.instant()
        subscriptionProfileRegistry
                .profile(subscription("sub1"), previousRebalanceTimestamp, new Weight(50d))
                .profile(subscription("sub2"), previousRebalanceTimestamp, new Weight(20d))
                .profile(subscription("sub3"), previousRebalanceTimestamp, new Weight(300d))
        consumerNodeLoadRegistry
                .operationsPerSecond(subscription("sub1"), ["c1": 100d, "c2": 100d])
                .operationsPerSecond(subscription("sub2"), ["c1": 10d, "c2": 10d])

        when:
        calculator.onBeforeBalancing(["c1", "c2", "c3"], [subscription("sub1"), subscription("sub2"), subscription("sub3")])

        then:
        assertThat(calculator.get(subscription("sub1")))
                .hasWeight(new Weight(100d))
        assertThat(calculator.get(subscription("sub2")))
                .hasWeight(new Weight(20d))
        assertThat(calculator.get(subscription("sub3")))
                .hasWeight(new Weight(300d))
    }

    def "should take 0 as the default weight"() {
        given:
        def previousRebalanceTimestamp = clock.instant()
        subscriptionProfileRegistry
                .profile(subscription("sub1"), previousRebalanceTimestamp, new Weight(50d))
                .profile(subscription("sub2"), previousRebalanceTimestamp, new Weight(20d))
                .profile(subscription("sub3"), previousRebalanceTimestamp, new Weight(300d))
        consumerNodeLoadRegistry
                .operationsPerSecond(subscription("sub1"), ["c1": 100d, "c2": 100d])
                .operationsPerSecond(subscription("sub2"), ["c1": 10d, "c2": 10d])

        when:
        calculator.onBeforeBalancing(["c1", "c2", "c3"], [subscription("sub1"), subscription("sub2")])

        then:
        assertThat(calculator.get(subscription("sub1")))
                .hasWeight(new Weight(100d))
        assertThat(calculator.get(subscription("sub2")))
                .hasWeight(new Weight(20d))
        assertThat(calculator.get(subscription("sub3")))
                .hasWeight(Weight.ZERO)
        assertThat(calculator.get(subscription("sub4")))
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
        calculator.onBeforeBalancing(["c1", "c2", "c3"], [subscription("sub1"), subscription("sub2")])

        when:
        calculator.onAfterBalancing(workDistributionChanges)

        then:
        assertThat(calculator.get(subscription("sub1")))
                .hasLastRebalanceTimestamp(clock.instant())
        assertThat(calculator.get(subscription("sub2")))
                .hasLastRebalanceTimestamp(previousRebalanceTimestamp)
    }

    def "should try decrease weight after defined weightWindowSize"() {
        given:
        def previousRebalanceTimestamp = clock.instant()
        subscriptionProfileRegistry
                .profile(subscription("sub1"), previousRebalanceTimestamp, new Weight(100d))
        consumerNodeLoadRegistry
                .operationsPerSecond(subscription("sub1"), ["c1": 5d, "c2": 5d])

        when:
        calculator.onBeforeBalancing(["c1", "c2"], [subscription("sub1")])

        then:
        assertThat(calculator.get(subscription("sub1")))
                .hasWeight(new Weight(100d))

        when:
        clock.advance(weightWindowSize.plusMinutes(1))

        and:
        calculator.onBeforeBalancing(["c1", "c2"], [subscription("sub1")])

        then:
        assertThat(calculator.get(subscription("sub1")))
                .hasWeight(new Weight(5d))
    }

    private static SubscriptionName subscription(String name) {
        return SubscriptionName.fromString("pl.allegro.tech.hermes\$$name")
    }
}
