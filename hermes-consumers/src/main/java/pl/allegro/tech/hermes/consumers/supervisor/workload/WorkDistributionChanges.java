package pl.allegro.tech.hermes.consumers.supervisor.workload;

import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.stream.Stream;
import pl.allegro.tech.hermes.api.SubscriptionName;

public class WorkDistributionChanges {

  private final SubscriptionAssignmentView deletions;
  private final SubscriptionAssignmentView additions;
  private final Set<String> modifiedConsumerNodes;

  WorkDistributionChanges(
      SubscriptionAssignmentView deletions, SubscriptionAssignmentView additions) {
    this.deletions = deletions;
    this.additions = additions;
    this.modifiedConsumerNodes =
        Sets.union(deletions.getConsumerNodes(), additions.getConsumerNodes());
  }

  int getDeletedAssignmentsCount() {
    return deletions.getAllAssignments().size();
  }

  int getCreatedAssignmentsCount() {
    return additions.getAllAssignments().size();
  }

  Set<String> getModifiedConsumerNodes() {
    return modifiedConsumerNodes;
  }

  public Set<SubscriptionName> getRebalancedSubscriptions() {
    return Stream.concat(
            additions.getSubscriptions().stream(), deletions.getSubscriptions().stream())
        .collect(toSet());
  }
}
