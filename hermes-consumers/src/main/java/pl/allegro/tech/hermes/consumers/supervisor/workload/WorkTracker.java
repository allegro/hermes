package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.List;

import static java.lang.String.format;

public class WorkTracker {

    private final String consumerNodeId;

    private final ConsumerWorkloadRegistry registry;
    private final SubscriptionAssignmentNotifyingCache assignmentCache;

    public WorkTracker(String consumerNodeId,
                       ConsumerWorkloadRegistry registry,
                       SubscriptionAssignmentNotifyingCache assignmentCache) {
        this.consumerNodeId = consumerNodeId;
        this.registry = registry;
        this.assignmentCache = assignmentCache;
    }

    public boolean isReady() {
        return assignmentCache.isStarted();
    }

    public void forceAssignment(Subscription subscription) {
        registry.addAssignment(new SubscriptionAssignment(
                consumerNodeId,
                subscription.getQualifiedName()
        ));
    }

    public void dropAssignment(Subscription subscription) {
        registry.dropAssignment(new SubscriptionAssignment(
                consumerNodeId,
                subscription.getQualifiedName()
        ));
    }

    public WorkDistributionChanges apply(SubscriptionAssignmentView initialState,
                                         SubscriptionAssignmentView targetView) {
        List<SubscriptionAssignment> assignmentDeletions = initialState.deletions(targetView).getAllAssignments();
        List<SubscriptionAssignment> assignmentAdditions = initialState.additions(targetView).getAllAssignments();

        assignmentDeletions.forEach(registry::dropAssignment);
        assignmentAdditions.forEach(registry::addAssignment);

        return new WorkDistributionChanges(assignmentDeletions.size(), assignmentAdditions.size());
    }

    public SubscriptionAssignmentView getAssignmentsSnapshot() {
        return assignmentCache.createSnapshot();
    }

    public boolean isAssignedTo(SubscriptionName subscription, String consumerNodeId) {
        return assignmentCache.isAssignedTo(consumerNodeId, subscription);
    }

    public static class WorkDistributionChanges {
        private final int assignmentsDeleted;
        private final int assignmentsCreated;

        public WorkDistributionChanges(int assignmentsDeleted, int assignmentsCreated) {
            this.assignmentsDeleted = assignmentsDeleted;
            this.assignmentsCreated = assignmentsCreated;
        }

        public int getDeletedAssignmentsCount() {
            return assignmentsDeleted;
        }

        public int getCreatedAssignmentsCount() {
            return assignmentsCreated;
        }

        public String toString() {
            return format("assignments_created=%d, assignments_deleted=%d",
                    assignmentsCreated, assignmentsDeleted);
        }
    }
}
