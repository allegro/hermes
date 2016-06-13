package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.google.common.collect.Sets;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

public class WorkTracker {

    private final String consumerNodeId;

    private final SubscriptionAssignmentRegistry registry;

    public WorkTracker(String consumerNodeId,
                       SubscriptionAssignmentRegistry registry) {
        this.consumerNodeId = consumerNodeId;
        this.registry = registry;
    }

    public void forceAssignment(Subscription subscription) {
        registry.addEphemeralAssignment(new SubscriptionAssignment(
                consumerNodeId,
                subscription.toSubscriptionName()
        ));
    }

    public void dropAssignment(Subscription subscription) {
        registry.dropAssignment(new SubscriptionAssignment(
                consumerNodeId,
                subscription.toSubscriptionName()
        ));
    }

    public WorkDistributionChanges apply(SubscriptionAssignmentView targetView) {
        SubscriptionAssignmentView currentView = getAssignments();

        List<SubscriptionAssignment> assignmentDeletions = currentView.deletions(targetView).getAllAssignments();
        List<SubscriptionAssignment> assignmentAdditions = currentView.additions(targetView).getAllAssignments();

        assignmentDeletions.forEach(registry::dropAssignment);
        assignmentAdditions.forEach(registry::addPersistentAssignment);

        Sets.SetView<SubscriptionName> removedSubscriptions = Sets.difference(currentView.getSubscriptions(), targetView.getSubscriptions());
        removedSubscriptions.forEach(registry::removeSubscriptionEntry);

        return new WorkDistributionChanges(assignmentDeletions.size(), assignmentAdditions.size(), removedSubscriptions.size());
    }

    public SubscriptionAssignmentView getAssignments() {
        return registry.createSnapshot();
    }

    public boolean isAssignedTo(SubscriptionName subscription, String consumerNodeId) {
        return registry.isAssignedTo(consumerNodeId, subscription);
    }

    public static class WorkDistributionChanges {
        private final int assignmentsDeleted;
        private final int assignmentsCreated;
        private final int subscriptionsDeleted;

        public WorkDistributionChanges(int assignmentsDeleted, int assignmentsCreated, int subscriptionsDeleted) {
            this.assignmentsDeleted = assignmentsDeleted;
            this.assignmentsCreated = assignmentsCreated;
            this.subscriptionsDeleted = subscriptionsDeleted;
        }

        public int getDeletedAssignmentsCount() {
            return assignmentsDeleted;
        }

        public int getCreatedAssignmentsCount() {
            return assignmentsCreated;
        }

        public String toString() {
            return format("assignments_created=%d, assignments_deleted=%d, subscriptions_deleted=%d",
                    assignmentsCreated, assignmentsDeleted, subscriptionsDeleted);
        }
    }
}
