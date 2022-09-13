package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted

import com.codahale.metrics.MetricFilter
import com.codahale.metrics.MetricRegistry
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkDistributionChanges
import pl.allegro.tech.hermes.metrics.PathsCompiler
import pl.allegro.tech.hermes.test.helper.time.ModifiableClock
import spock.lang.Specification
import spock.lang.Subject

import java.time.Duration

import static pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.WeightedWorkloadAssertions.assertThat

class WeightedWorkBalancingListenerTest extends Specification {

    def clock = new ModifiableClock()
    def workDistributionChanges = Mock(WorkDistributionChanges)
    def consumerNodeLoadRegistry = new MockConsumerNodeLoadRegistry()
    def subscriptionProfileRegistry = new MockSubscriptionProfileRegistry()
    def weightWindowSize = Duration.ofMinutes(1)
    def currentLoadProvider = new CurrentLoadProvider()
    def metricsRegistry = new MetricRegistry()
    def metrics = new WeightedWorkloadMetrics(new HermesMetrics(metricsRegistry, new PathsCompiler("host")))

    @Subject
    def listener = new WeightedWorkBalancingListener(
            consumerNodeLoadRegistry,
            subscriptionProfileRegistry,
            currentLoadProvider,
            metrics,
            clock,
            weightWindowSize
    )

    def cleanup() {
        consumerNodeLoadRegistry.reset()
        subscriptionProfileRegistry.reset()
        currentLoadProvider.clear()
    }

    def "should take the maximal value of operations per second as the subscription weight"() {
        given:
        consumerNodeLoadRegistry
                .operationsPerSecond(subscription("sub1"), ["c1": 500d, "c2": 10d])
                .operationsPerSecond(subscription("sub2"), ["c1": 500d, "c2": 10d])
                .operationsPerSecond(subscription("sub3"), ["c3": 10d, "c4": 10d])

        when:
        listener.onBeforeBalancing(["c1", "c2", "c3"])

        then:
        assertSubscriptionWeight(subscription("sub1"), new Weight(500d))
        assertSubscriptionWeight(subscription("sub2"), new Weight(500d))
        assertSubscriptionWeight(subscription("sub3"), new Weight(10d))
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
        listener.onBeforeBalancing(["c1", "c2"])

        then:
        assertSubscriptionWeight(subscription("sub1"), new Weight(80.32653298563167d))

        when:
        workDistributionChanges.getRebalancedSubscriptions() >> []
        listener.onAfterBalancing(workDistributionChanges)
        clock.advance(weightWindowSize.minusSeconds(30))
        listener.onBeforeBalancing(["c1", "c2"])

        then:
        assertSubscriptionWeight(subscription("sub1"), new Weight(68.39397205857212d))
    }

    def "should return previous weight when the new timestamp is before the previous one"() {
        given:
        def previousRebalanceTimestamp = clock.instant()
        subscriptionProfileRegistry
                .updateTimestamp(previousRebalanceTimestamp)
                .profile(subscription("sub1"), previousRebalanceTimestamp, new Weight(100d))
        consumerNodeLoadRegistry
                .operationsPerSecond(subscription("sub1"), ["c1": 50d, "c2": 50d])

        when:
        clock.advanceMinutes(-1)
        listener.onBeforeBalancing(["c1", "c2"])

        then:
        assertSubscriptionWeight(subscription("sub1"), new Weight(100d))
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
        listener.onBeforeBalancing(["c1", "c2"])

        then:
        assertSubscriptionWeight(subscription("sub1"), new Weight(80.32653298563167d))
        assertSubscriptionWeight(subscription("sub2"), Weight.ZERO)
        assertSubscriptionWeight(subscription("sub3"), Weight.ZERO)
    }

    def "should take value of operations per second reported by consumers as initial weight"() {
        given:
        subscriptionProfileRegistry
                .persist(SubscriptionProfiles.EMPTY)
        consumerNodeLoadRegistry
                .operationsPerSecond(subscription("sub1"), ["c1": 50d, "c2": 50d])

        when:
        listener.onBeforeBalancing(["c1", "c2"])

        then:
        assertSubscriptionWeight(subscription("sub1"), new Weight(50d))
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
        listener.onBeforeBalancing(["c1", "c2", "c3"])

        when:
        listener.onAfterBalancing(workDistributionChanges)

        then:
        def profiles = subscriptionProfileRegistry.fetch()
        assertThat(profiles.getProfile(subscription("sub1")))
                .hasLastRebalanceTimestamp(clock.instant())
        assertThat(profiles.getProfile(subscription("sub2")))
                .hasLastRebalanceTimestamp(previousRebalanceTimestamp)
    }

    def "should unregister workload metrics for inactive consumers"() {
        given:
        metrics.reportCurrentScore("c1", 0.5d)
        metrics.reportCurrentScore("c2", 1.5d)

        when:
        listener.onBeforeBalancing(["c2"])

        then:
        metricsRegistry.getGauges(MetricFilter.contains(".c2.")).size() == 1
        metricsRegistry.getGauges(MetricFilter.contains(".c1.")).size() == 0
    }

    def "should unregister workload metrics when the consumer is no longer a leader"() {
        given:
        metrics.reportCurrentScore("c1", 0.5d)
        metrics.reportCurrentScore("c2", 1.5d)

        when:
        listener.onBalancingSkipped()

        then:
        metricsRegistry.getGauges().size() == 0
    }

    private static SubscriptionName subscription(String name) {
        return SubscriptionName.fromString("pl.allegro.tech.hermes\$$name")
    }

    private void assertSubscriptionWeight(SubscriptionName subscriptionName, Weight weight) {
        def profiles = currentLoadProvider.getProfiles()
        assertThat(profiles.getProfile(subscriptionName)).hasWeight(weight)
    }
}
