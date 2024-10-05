package pl.allegro.tech.hermes.consumers.supervisor.workload;

import java.util.List;
import pl.allegro.tech.hermes.api.SubscriptionName;

public interface WorkBalancer {

  WorkBalancingResult balance(
      List<SubscriptionName> subscriptions,
      List<String> activeConsumerNodes,
      SubscriptionAssignmentView currentState,
      WorkloadConstraints constraints);
}
