package pl.allegro.tech.hermes.consumers.supervisor.workload.selective;

import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentView;

import static java.lang.String.format;

public class WorkBalancingResult {
    private SubscriptionAssignmentView state;
    private Counts subscriptionsCounts;
    private Counts consumersCounts;
    private int missingResources;

    private WorkBalancingResult(Builder builder) {
        this.state = builder.state;
        this.subscriptionsCounts = builder.subscriptionsCounts;
        this.consumersCounts = builder.consumersCounts;
        this.missingResources = builder.missingResources;
    }

    public SubscriptionAssignmentView getAssignmentsView() {
        return state;
    }

    public int getRemovedSubscriptionsCount() {
        return subscriptionsCounts.inactiveCount;
    }

    public int getRemovedSupervisorsCount() {
        return consumersCounts.inactiveCount;
    }

    public String toString() {
        return format("subscriptions_stats %s, consumers_stats %s, missing_resources=%s", subscriptionsCounts, consumersCounts, missingResources);
    }

    public int getMissingResources() {
        return missingResources;
    }

    private static class Counts {
        int activeCount, inactiveCount, newCount;

        public Counts() {}

        public Counts(int activeCount, int inactiveCount, int newCount) {
            this.activeCount = activeCount;
            this.inactiveCount = inactiveCount;
            this.newCount = newCount;
        }

        @Override
        public String toString() {
            return format("[activeCount=%s, inactiveCount=%s, newCount=%s]", activeCount, inactiveCount, newCount);
        }
    }

    public static class Builder {
        private SubscriptionAssignmentView state;
        private Counts subscriptionsCounts = new Counts();
        private Counts consumersCounts = new Counts();
        private int missingResources;

        public Builder(SubscriptionAssignmentView state) {
            this.state = state;
        }

        public Builder withSubscriptionsStats(int activeCount, int inactiveCount, int newCount) {
            this.subscriptionsCounts = new Counts(activeCount, inactiveCount, newCount);
            return this;
        }

        public Builder withConsumersStats(int activeCount, int inactiveCount, int newCount) {
            this.consumersCounts = new Counts(activeCount, inactiveCount, newCount);
            return this;
        }

        public Builder withMissingResources(int missingResources) {
            this.missingResources = missingResources;
            return this;
        }

        public WorkBalancingResult build() {
            return new WorkBalancingResult(this);
        }
    }
}
