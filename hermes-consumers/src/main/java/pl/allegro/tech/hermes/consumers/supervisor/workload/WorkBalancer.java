package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.List;

public interface WorkBalancer {

    WorkBalancingResult balance(List<SubscriptionName> subscriptions,
                                List<String> activeConsumerNodes,
                                SubscriptionAssignmentView currentState,
                                WorkloadConstraints constraints);
}
