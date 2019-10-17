package pl.allegro.tech.hermes.consumers.supervisor.workload;

public interface ConsumerAssignmentRegistry {

    WorkDistributionChanges updateAssignments(SubscriptionAssignmentView initialState, SubscriptionAssignmentView targetState);
}
