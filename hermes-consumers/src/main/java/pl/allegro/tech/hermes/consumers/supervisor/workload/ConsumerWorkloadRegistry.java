package pl.allegro.tech.hermes.consumers.supervisor.workload;

public interface ConsumerWorkloadRegistry {

    void dropAssignment(SubscriptionAssignment subscriptionAssignment);

    void addAssignment(SubscriptionAssignment subscriptionAssignment);
}
