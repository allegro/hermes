package pl.allegro.tech.hermes.consumers.supervisor.workload;

import java.util.Collections;
import pl.allegro.tech.hermes.api.SubscriptionName;

public class SubscriptionAssignmentViewBuilder {

  private SubscriptionAssignmentView assignmentView;

  public SubscriptionAssignmentViewBuilder() {
    assignmentView = new SubscriptionAssignmentView(Collections.emptyMap());
  }

  public SubscriptionAssignmentView build() {
    return SubscriptionAssignmentView.copyOf(assignmentView);
  }

  public SubscriptionAssignmentViewBuilder withAssignment(
      SubscriptionName subscriptionName, String... consumerNodeIds) {
    for (String consumerNodeId : consumerNodeIds) {
      assignmentView =
          assignmentView.transform(
              (view, transformer) -> {
                transformer.addSubscription(subscriptionName);
                transformer.addConsumerNode(consumerNodeId);
                transformer.addAssignment(
                    new SubscriptionAssignment(consumerNodeId, subscriptionName));
              });
    }
    return this;
  }
}
