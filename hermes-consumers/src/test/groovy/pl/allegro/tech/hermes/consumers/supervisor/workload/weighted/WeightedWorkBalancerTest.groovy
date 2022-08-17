package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted

import pl.allegro.tech.hermes.api.Constraints
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentViewBuilder
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkloadConstraints
import pl.allegro.tech.hermes.test.helper.time.ModifiableClock
import spock.lang.Specification

import static java.time.Duration.ofHours
import static pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.WeightedWorkloadAssertions.assertThat

class WeightedWorkBalancerTest extends Specification {

    def clock = new ModifiableClock()

    def "should balance taking into account subscription weight"() {
        given:
        def previousRebalanceTimestamp = clock.instant()
        def subscriptionProfileRegistry = new MockSubscriptionProfileRegistry()
                .profile(subscription("sub1"), previousRebalanceTimestamp, new Weight(500d))
                .profile(subscription("sub2"), previousRebalanceTimestamp, new Weight(500d))
                .profile(subscription("sub3"), previousRebalanceTimestamp, new Weight(10d))
                .profile(subscription("sub4"), previousRebalanceTimestamp, new Weight(10d))
        def stabilizationWindow = ofHours(1)
        def balancer = new WeightedWorkBalancer(clock, stabilizationWindow, 0d, subscriptionProfileRegistry)
        def initial = new SubscriptionAssignmentViewBuilder()
                .withAssignment(subscription("sub1"), "c1", "c2")
                .withAssignment(subscription("sub2"), "c1", "c2")
                .withAssignment(subscription("sub3"), "c3", "c4")
                .withAssignment(subscription("sub4"), "c3", "c4")
                .build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(2)
                .withMaxSubscriptionsPerConsumer(3)
                .withActiveConsumers(initial.consumerNodes.size())
                .build()

        and: "make sure that subscriptions are eligible for rebalancing"
        clock.advance(stabilizationWindow.plusMinutes(1))

        when:
        def balanced = balancer.balance(initial.subscriptions as List, initial.consumerNodes as List, initial, constraints)

        then:
        // c1 = sub1+sub3 = 500+10 = 510
        // c2 = sub1+sub3 = 500+10 = 510
        // c3 = sub2+sub4 = 500+10 = 510
        // c4 = sub2+sub4 = 500+10 = 510
        assertThat(balanced)
                .hasAssignments(subscription("sub1"), "c1", "c2")
                .hasAssignments(subscription("sub2"), "c3", "c4")
                .hasAssignments(subscription("sub3"), "c1", "c2")
                .hasAssignments(subscription("sub4"), "c3", "c4")
    }

    def "should handle weight equal to 0"() {
        given:
        def previousRebalanceTimestamp = clock.instant()
        def subscriptionProfileRegistry = new MockSubscriptionProfileRegistry()
                .profile(subscription("sub1"), previousRebalanceTimestamp, Weight.ZERO)
                .profile(subscription("sub2"), previousRebalanceTimestamp, Weight.ZERO)
                .profile(subscription("sub3"), previousRebalanceTimestamp, Weight.ZERO)
                .profile(subscription("sub4"), previousRebalanceTimestamp, Weight.ZERO)
        def stabilizationWindow = ofHours(1)
        def balancer = new WeightedWorkBalancer(clock, stabilizationWindow, 0d, subscriptionProfileRegistry)
        def initial = new SubscriptionAssignmentViewBuilder()
                .withAssignment(subscription("sub1"), "c1", "c2")
                .withAssignment(subscription("sub2"), "c1", "c2")
                .withAssignment(subscription("sub3"), "c1", "c2")
                .withAssignment(subscription("sub4"), "c3", "c4")
                .build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(2)
                .withMaxSubscriptionsPerConsumer(3)
                .withActiveConsumers(initial.consumerNodes.size())
                .build()

        and: "make sure that subscriptions are eligible for rebalancing"
        clock.advance(stabilizationWindow.plusMinutes(1))

        when:
        def balanced = balancer.balance(initial.subscriptions as List, initial.consumerNodes as List, initial, constraints)

        then:
        assertThat(balanced)
                .hasAssignments(subscription("sub1"), "c1", "c2")
                .hasAssignments(subscription("sub2"), "c3", "c4")
                .hasAssignments(subscription("sub3"), "c1", "c2")
                .hasAssignments(subscription("sub4"), "c3", "c4")
    }

    def "should assign unassigned subscriptions to least loaded consumers"() {
        given:
        def previousRebalanceTimestamp = clock.instant()
        def subscriptionProfileRegistry = new MockSubscriptionProfileRegistry()
                .profile(subscription("sub1"), previousRebalanceTimestamp, new Weight(500d))
                .profile(subscription("sub2"), previousRebalanceTimestamp, new Weight(500d))
                .profile(subscription("sub3"), previousRebalanceTimestamp, new Weight(10d))
                .profile(subscription("sub4"), previousRebalanceTimestamp, new Weight(10d))
                .profile(subscription("sub5"), previousRebalanceTimestamp, new Weight(490d))
        def balancer = new WeightedWorkBalancer(clock, ofHours(1), 0d, subscriptionProfileRegistry)
        def initial = new SubscriptionAssignmentViewBuilder()
                .withAssignment(subscription("sub1"), "c3")
                .withAssignment(subscription("sub2"), "c2")
                .withAssignment(subscription("sub3"), "c1")
                .build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(1)
                .withMaxSubscriptionsPerConsumer(3)
                .withActiveConsumers(initial.consumerNodes.size())
                .build()

        when:
        def balanced = balancer.balance(subscriptionProfileRegistry.subscriptionNames as List, initial.consumerNodes as List, initial, constraints)

        then:
        // c1 = sub3+sub5 = 10+490 = 500
        // c2 = sub2 = 500
        // c3 = sub1+sub4 = 500+10 = 510
        assertThat(balanced)
                .hasAssignments(subscription("sub1"), "c3")
                .hasAssignments(subscription("sub2"), "c2")
                .hasAssignments(subscription("sub3"), "c1")
                .hasAssignments(subscription("sub4"), "c3")
                .hasAssignments(subscription("sub5"), "c1")
    }

    def "should not overload underloaded node while rebalancing"() {
        given:
        def previousRebalanceTimestamp = clock.instant()
        def subscriptionProfileRegistry = new MockSubscriptionProfileRegistry()
                .profile(subscription("sub1"), previousRebalanceTimestamp, new Weight(500d))
                .profile(subscription("sub2"), previousRebalanceTimestamp, new Weight(500d))
                .profile(subscription("sub3"), previousRebalanceTimestamp, new Weight(10d))
        def stabilizationWindow = ofHours(1)
        def balancer = new WeightedWorkBalancer(clock, stabilizationWindow, 0d, subscriptionProfileRegistry)
        def initial = new SubscriptionAssignmentViewBuilder()
                .withAssignment(subscription("sub1"), "c1")
                .withAssignment(subscription("sub2"), "c2")
                .withAssignment(subscription("sub3"), "c3")
                .build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(1)
                .withMaxSubscriptionsPerConsumer(3)
                .withActiveConsumers(initial.consumerNodes.size())
                .build()

        and: "make sure that subscriptions are eligible for rebalancing"
        clock.advance(stabilizationWindow.plusMinutes(1))

        when:
        def balanced = balancer.balance(subscriptionProfileRegistry.subscriptionNames as List, initial.consumerNodes as List, initial, constraints)

        then:
        assertThat(balanced)
                .hasAssignments(subscription("sub1"), "c1")
                .hasAssignments(subscription("sub2"), "c2")
                .hasAssignments(subscription("sub3"), "c3")
    }

    def "should not rebalance when change is insignificant"() {
        given:
        def previousRebalanceTimestamp = clock.instant()
        def subscriptionProfileRegistry = new MockSubscriptionProfileRegistry()
                .profile(subscription("sub1"), previousRebalanceTimestamp, new Weight(5d))
                .profile(subscription("sub2"), previousRebalanceTimestamp, new Weight(500d))
                .profile(subscription("sub3"), previousRebalanceTimestamp, new Weight(10d))
                .profile(subscription("sub4"), previousRebalanceTimestamp, new Weight(10d))
        def stabilizationWindow = ofHours(1)
        def minSignificantChangePercent = 10.0d
        def balancer = new WeightedWorkBalancer(clock, stabilizationWindow, minSignificantChangePercent, subscriptionProfileRegistry)
        def initial = new SubscriptionAssignmentViewBuilder()
                .withAssignment(subscription("sub1"), "c1", "c2")
                .withAssignment(subscription("sub2"), "c3", "c4")
                .withAssignment(subscription("sub3"), "c1", "c2")
                .withAssignment(subscription("sub4"), "c3", "c4")
                .build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(2)
                .withMaxSubscriptionsPerConsumer(3)
                .withActiveConsumers(initial.consumerNodes.size())
                .build()

        and: "make sure that subscriptions are eligible for rebalancing"
        clock.advance(stabilizationWindow.plusMinutes(1))

        when:
        def balanced = balancer.balance(initial.subscriptions as List, initial.consumerNodes as List, initial, constraints)

        then:
        // Initial weights:
        // c1 = sub1+sub3 = 5+10 = 15
        // c2 = sub1+sub3 = 5+10 = 15
        // c3 = sub2+sub4 = 500+10 = 510
        // c4 = sub2+sub4 = 500+10 = 510
        //
        // The best we can do in this scenario is to swap sub1 with sub4:
        // c1 = sub4+sub3 = 10+10 = 20
        // c2 = sub4+sub3 = 10+10 = 20
        // c3 = sub2+sub1 = 500+5 = 505
        // c4 = sub2+sub1 = 500+5 = 505
        //
        // In this case, the percentage change is equal to:
        // (510 - 505) / 510 * 100% = 0.98%
        // which is less than the defined minSignificantChangePercent = 10%
        balanced.assignmentsView == initial
    }

    def "should not rebalance if the stabilization window has not elapsed yet"() {
        given:
        def previousRebalanceTimestamp = clock.instant()
        def subscriptionProfileRegistry = new MockSubscriptionProfileRegistry()
                .profile(subscription("sub1"), previousRebalanceTimestamp, new Weight(500d))
                .profile(subscription("sub2"), previousRebalanceTimestamp, new Weight(500d))
                .profile(subscription("sub3"), previousRebalanceTimestamp, new Weight(10d))
                .profile(subscription("sub4"), previousRebalanceTimestamp, new Weight(10d))
        def stabilizationWindow = ofHours(1)
        def balancer = new WeightedWorkBalancer(clock, stabilizationWindow, 0d, subscriptionProfileRegistry)
        def initial = new SubscriptionAssignmentViewBuilder()
                .withAssignment(subscription("sub1"), "c1", "c2")
                .withAssignment(subscription("sub2"), "c1", "c2")
                .withAssignment(subscription("sub3"), "c3", "c4")
                .withAssignment(subscription("sub4"), "c3", "c4")
                .build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(2)
                .withMaxSubscriptionsPerConsumer(3)
                .withActiveConsumers(initial.consumerNodes.size())
                .build()

        and:
        clock.advance(stabilizationWindow.minusMinutes(1))

        when:
        def balanced = balancer.balance(initial.subscriptions as List, initial.consumerNodes as List, initial, constraints)

        then:
        assertThat(balanced)
                .hasAssignments(subscription("sub1"), "c1", "c2")
                .hasAssignments(subscription("sub2"), "c1", "c2")
                .hasAssignments(subscription("sub3"), "c3", "c4")
                .hasAssignments(subscription("sub4"), "c3", "c4")
    }

    def "should rebalance if the stabilization window has elapsed"() {
        given:
        def previousRebalanceTimestamp = clock.instant()
        def subscriptionProfileRegistry = new MockSubscriptionProfileRegistry()
                .profile(subscription("sub1"), previousRebalanceTimestamp, new Weight(500d))
                .profile(subscription("sub2"), previousRebalanceTimestamp, new Weight(500d))
                .profile(subscription("sub3"), previousRebalanceTimestamp, new Weight(10d))
                .profile(subscription("sub4"), previousRebalanceTimestamp, new Weight(10d))
        def stabilizationWindow = ofHours(1)
        def balancer = new WeightedWorkBalancer(clock, stabilizationWindow, 0d, subscriptionProfileRegistry)
        def initial = new SubscriptionAssignmentViewBuilder()
                .withAssignment(subscription("sub1"), "c1", "c2")
                .withAssignment(subscription("sub2"), "c1", "c2")
                .withAssignment(subscription("sub3"), "c3", "c4")
                .withAssignment(subscription("sub4"), "c3", "c4")
                .build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(2)
                .withMaxSubscriptionsPerConsumer(3)
                .withActiveConsumers(initial.consumerNodes.size())
                .build()

        and:
        clock.advance(stabilizationWindow.plusMinutes(1))

        when:
        def balanced = balancer.balance(initial.subscriptions as List, initial.consumerNodes as List, initial, constraints)

        then:
        assertThat(balanced)
                .hasAssignments(subscription("sub1"), "c1", "c2")
                .hasAssignments(subscription("sub2"), "c3", "c4")
                .hasAssignments(subscription("sub3"), "c1", "c2")
                .hasAssignments(subscription("sub4"), "c3", "c4")
    }

    def "should not change assignments when rebalance is not needed"() {
        given:
        def balancer = new WeightedWorkBalancer(clock, ofHours(1), 0d, new MockSubscriptionProfileRegistry())
        def initial = new SubscriptionAssignmentViewBuilder()
                .withAssignment(subscription("sub1"), "c1", "c2")
                .withAssignment(subscription("sub2"), "c2", "c3")
                .withAssignment(subscription("sub3"), "c1", "c3")
                .build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(2)
                .withMaxSubscriptionsPerConsumer(2)
                .withActiveConsumers(initial.consumerNodes.size())
                .build()

        when:
        def balanced = balancer.balance(initial.subscriptions as List, initial.consumerNodes as List, initial, constraints)

        then:
        balanced.assignmentsView == initial
    }

    def "should remove an inactive subscriptions"() {
        given:
        def balancer = new WeightedWorkBalancer(clock, ofHours(1), 0d, new MockSubscriptionProfileRegistry())
        def initial = new SubscriptionAssignmentViewBuilder()
                .withAssignment(subscription("sub1"), "c1", "c2")
                .withAssignment(subscription("sub2"), "c2", "c3")
                .withAssignment(subscription("sub3"), "c1", "c3")
                .build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(2)
                .withMaxSubscriptionsPerConsumer(2)
                .withActiveConsumers(initial.consumerNodes.size())
                .build()

        when:
        def balanced = balancer.balance([subscription("sub1"), subscription("sub2")], initial.consumerNodes as List, initial, constraints)

        then:
        assertThat(balanced)
                .hasSubscriptions(subscription("sub1"), subscription("sub2"))
                .hasConsumers(initial.consumerNodes)
                .hasAssignments(subscription("sub1"), "c1", "c2")
                .hasAssignments(subscription("sub2"), "c2", "c3")
    }

    def "should remove an inactive consumer"() {
        given:
        def balancer = new WeightedWorkBalancer(clock, ofHours(1), 0d, new MockSubscriptionProfileRegistry())
        def initial = new SubscriptionAssignmentViewBuilder()
                .withAssignment(subscription("sub1"), "c1", "c2")
                .withAssignment(subscription("sub2"), "c2", "c3")
                .withAssignment(subscription("sub3"), "c1", "c3")
                .build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(2)
                .withMaxSubscriptionsPerConsumer(3)
                .withActiveConsumers(initial.consumerNodes.size())
                .build()

        when:
        def balanced = balancer.balance(initial.subscriptions as List, ["c1", "c2"], initial, constraints)

        then:
        assertThat(balanced)
                .hasSubscriptions(initial.subscriptions)
                .hasConsumers("c1", "c2")
                .hasAssignments(subscription("sub1"), "c1", "c2")
                .hasAssignments(subscription("sub2"), "c1", "c2")
                .hasAssignments(subscription("sub3"), "c1", "c2")
    }

    def "should assign subscriptions to a new consumer node"() {
        given:
        def balancer = new WeightedWorkBalancer(clock, ofHours(1), 0d, new MockSubscriptionProfileRegistry())
        def initial = new SubscriptionAssignmentViewBuilder()
                .withAssignment(subscription("sub1"), "c1")
                .withAssignment(subscription("sub2"), "c2")
                .withAssignment(subscription("sub3"), "c3")
                .withAssignment(subscription("sub4"), "c1")
                .build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(1)
                .withMaxSubscriptionsPerConsumer(2)
                .withActiveConsumers(initial.consumerNodes.size())
                .build()

        when:
        def balanced = balancer.balance(initial.subscriptions as List, ["c1", "c2", "c3", "c4"], initial, constraints)

        then:
        assertThat(balanced)
                .hasSubscriptions(initial.subscriptions)
                .hasConsumers("c1", "c2", "c3", "c4")
                .hasAssignments(subscription("sub1"), "c1")
                .hasAssignments(subscription("sub2"), "c2")
                .hasAssignments(subscription("sub3"), "c3")
                .hasAssignments(subscription("sub4"), "c4")
    }

    def "should respect limit of subscriptions per consumer"() {
        given:
        def balancer = new WeightedWorkBalancer(clock, ofHours(1), 0d, new MockSubscriptionProfileRegistry())
        def initial = new SubscriptionAssignmentViewBuilder()
                .withAssignment(subscription("sub1"), "c1", "c2")
                .withAssignment(subscription("sub2"), "c1", "c2")
                .build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(2)
                .withMaxSubscriptionsPerConsumer(2)
                .withActiveConsumers(initial.consumerNodes.size())
                .build()

        when:
        def balanced = balancer.balance([subscription("sub1"), subscription("sub2"), subscription("sub3")], initial.consumerNodes as List, initial, constraints)

        then:
        assertThat(balanced)
                .hasMissingResources(2)
                .hasSubscriptions(subscription("sub1"), subscription("sub2"))
                .hasConsumers(initial.consumerNodes)
                .hasAssignments(subscription("sub1"), "c1", "c2")
                .hasAssignments(subscription("sub2"), "c1", "c2")
    }

    def "should respect workload constraints"() {
        given:
        def balancer = new WeightedWorkBalancer(clock, ofHours(1), 0d, new MockSubscriptionProfileRegistry())
        def topic = TopicName.fromQualifiedName("pl.allegro.tech.hermes")
        def initial = new SubscriptionAssignmentViewBuilder()
                .withAssignment(subscription("sub1", topic), "c1", "c2")
                .withAssignment(subscription("sub2", topic), "c2", "c3")
                .withAssignment(subscription("sub3", topic), "c1", "c3")
                .build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(2)
                .withMaxSubscriptionsPerConsumer(3)
                .withActiveConsumers(initial.consumerNodes.size())
                .withTopicConstraints([(topic): new Constraints(1)])
                .withSubscriptionConstraints([(subscription("sub1", topic)): new Constraints(3)])
                .build()

        when:
        def balanced = balancer.balance(initial.subscriptions as List, ["c1", "c2", "c3"], initial, constraints)

        then:
        assertThat(balanced)
                .hasSubscriptions(initial.subscriptions)
                .hasConsumers("c1", "c2", "c3")
                .hasAssignments(subscription("sub1"), "c1", "c2", "c3")
                .hasAssignments(subscription("sub2"), "c1")
                .hasAssignments(subscription("sub3"), "c3")
    }

    def "should balance work for one consumer node"() {
        given:
        def balancer = new WeightedWorkBalancer(clock, ofHours(1), 0d, new MockSubscriptionProfileRegistry())
        def initial = new SubscriptionAssignmentViewBuilder().build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(2)
                .withMaxSubscriptionsPerConsumer(2)
                .withActiveConsumers(1)
                .build()

        when:
        def balanced = balancer.balance([subscription("sub1"), subscription("sub2")], ["c1"], initial, constraints)

        then:
        assertThat(balanced)
                .hasSubscriptions(subscription("sub1"), subscription("sub2"))
                .hasConsumers("c1")
                .hasAssignments(subscription("sub1"), "c1")
                .hasAssignments(subscription("sub2"), "c1")
    }

    def "should balance one subscription"() {
        given:
        def balancer = new WeightedWorkBalancer(clock, ofHours(1), 0d, new MockSubscriptionProfileRegistry())
        def initial = new SubscriptionAssignmentViewBuilder().build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(2)
                .withMaxSubscriptionsPerConsumer(2)
                .withActiveConsumers(2)
                .build()

        when:
        def balanced = balancer.balance([subscription("sub1")], ["c1", "c2"], initial, constraints)

        then:
        assertThat(balanced)
                .hasSubscriptions(subscription("sub1"))
                .hasConsumers("c1", "c2")
                .hasAssignments(subscription("sub1"), "c1", "c2")
    }

    def "should not create assignments when there are no consumers"() {
        given:
        def balancer = new WeightedWorkBalancer(clock, ofHours(1), 0d, new MockSubscriptionProfileRegistry())
        def initial = new SubscriptionAssignmentViewBuilder().build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(2)
                .withMaxSubscriptionsPerConsumer(2)
                .withActiveConsumers(0)
                .build()

        when:
        def balanced = balancer.balance([subscription("sub1")], [], initial, constraints)

        then:
        assertThat(balanced)
                .hasMissingResources(0)
                .hasNoAssignments()
    }

    def "should not create assignments when there are no subscriptions"() {
        given:
        def balancer = new WeightedWorkBalancer(clock, ofHours(1), 0d, new MockSubscriptionProfileRegistry())
        def initial = new SubscriptionAssignmentViewBuilder().build()
        def constraints = WorkloadConstraints.builder()
                .withConsumersPerSubscription(2)
                .withMaxSubscriptionsPerConsumer(2)
                .withActiveConsumers(2)
                .build()

        when:
        def balanced = balancer.balance([], ["c1", "c2"], initial, constraints)

        then:
        assertThat(balanced)
                .hasMissingResources(0)
                .hasNoAssignments()
    }

    private static SubscriptionName subscription(String name) {
        return SubscriptionName.fromString("pl.allegro.tech.hermes\$$name")
    }

    private static SubscriptionName subscription(String name, TopicName topicName) {
        return new SubscriptionName(name, topicName)
    }
}
