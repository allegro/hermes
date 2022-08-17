package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted

import org.assertj.core.api.Assertions
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkBalancingResult

import java.time.Instant

class WeightedWorkloadAssertions extends Assertions {

    static WorkBalancingResultAssert assertThat(WorkBalancingResult result) {
        return new WorkBalancingResultAssert(result)
    }

    static SubscriptionProfileAssert assertThat(SubscriptionProfile profile) {
        return new SubscriptionProfileAssert(profile)
    }

    static class SubscriptionProfileAssert {
        private final SubscriptionProfile profile

        SubscriptionProfileAssert(SubscriptionProfile profile) {
            this.profile = profile
        }

        SubscriptionProfileAssert hasWeight(Weight weight) {
            assertThat(profile.weight).isEqualTo(weight)
            return this
        }

        SubscriptionProfileAssert hasLastRebalanceTimestamp(Instant timestamp) {
            assertThat(profile.lastRebalanceTimestamp).isEqualTo(timestamp)
            return this
        }
    }

    static class WorkBalancingResultAssert {
        private final WorkBalancingResult result

        WorkBalancingResultAssert(WorkBalancingResult result) {
            this.result = result
        }

        WorkBalancingResultAssert hasAssignments(SubscriptionName subscriptionName, String... consumerNodeIds) {
            assertThat(result.assignmentsView.getConsumerNodesForSubscription(subscriptionName)).isEqualTo(consumerNodeIds as Set)
            return this
        }

        WorkBalancingResultAssert hasNoAssignments() {
            assertThat(result.assignmentsView.getAllAssignments()).isEmpty()
            return this
        }

        WorkBalancingResultAssert hasSubscriptions(SubscriptionName... subscriptionNames) {
            assertThat(result.assignmentsView.subscriptions).isEqualTo(subscriptionNames as Set)
            return this
        }

        WorkBalancingResultAssert hasSubscriptions(Collection<SubscriptionName> subscriptionNames) {
            assertThat(result.assignmentsView.subscriptions).isEqualTo(subscriptionNames as Set)
            return this
        }

        WorkBalancingResultAssert hasConsumers(String... consumerNodeIds) {
            assertThat(result.assignmentsView.consumerNodes).isEqualTo(consumerNodeIds as Set)
            return this
        }

        WorkBalancingResultAssert hasConsumers(Collection<String> consumerNodeIds) {
            assertThat(result.assignmentsView.consumerNodes).isEqualTo(consumerNodeIds as Set)
            return this
        }

        WorkBalancingResultAssert hasMissingResources(int missingResources) {
            assertThat(result.missingResources).isEqualTo(missingResources)
            return this
        }
    }
}
