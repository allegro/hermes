package pl.allegro.tech.hermes.consumers.supervisor.workload.selective;

import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignment;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentView;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkloadConstraints;

class AvailableWork extends Spliterators.AbstractSpliterator<SubscriptionAssignment> {
  private final SubscriptionAssignmentView state;
  private final WorkloadConstraints constraints;

  private AvailableWork(SubscriptionAssignmentView state, WorkloadConstraints constraints) {
    super(Long.MAX_VALUE, 0);
    this.state = state;
    this.constraints = constraints;
  }

  @Override
  public boolean tryAdvance(Consumer<? super SubscriptionAssignment> action) {
    Set<String> availableConsumers = availableConsumerNodes(state);
    if (!availableConsumers.isEmpty()) {
      Optional<SubscriptionAssignment> subscriptionAssignment =
          getNextSubscription(state, availableConsumers)
              .map(
                  subscription ->
                      getNextSubscriptionAssignment(state, availableConsumers, subscription));
      if (subscriptionAssignment.isPresent()) {
        action.accept(subscriptionAssignment.get());
        return true;
      }
    }
    return false;
  }

  private Optional<SubscriptionName> getNextSubscription(
      SubscriptionAssignmentView state, Set<String> availableConsumerNodes) {
    return state.getSubscriptions().stream()
        .filter(s -> state.getAssignmentsCountForSubscription(s) < constraints.getConsumerCount(s))
        .filter(
            s ->
                !Sets.difference(availableConsumerNodes, state.getConsumerNodesForSubscription(s))
                    .isEmpty())
        .min(Comparator.comparingInt(state::getAssignmentsCountForSubscription));
  }

  private SubscriptionAssignment getNextSubscriptionAssignment(
      SubscriptionAssignmentView state,
      Set<String> availableConsumerNodes,
      SubscriptionName subscriptionName) {
    return availableConsumerNodes.stream()
        .filter(s -> !state.getSubscriptionsForConsumerNode(s).contains(subscriptionName))
        .min(Comparator.comparingInt(state::getAssignmentsCountForConsumerNode))
        .map(s -> new SubscriptionAssignment(s, subscriptionName))
        .get();
  }

  private Set<String> availableConsumerNodes(SubscriptionAssignmentView state) {
    return state.getConsumerNodes().stream()
        .filter(
            s ->
                state.getAssignmentsCountForConsumerNode(s)
                    < constraints.getMaxSubscriptionsPerConsumer())
        .filter(s -> state.getAssignmentsCountForConsumerNode(s) < state.getSubscriptionsCount())
        .collect(toSet());
  }

  static Stream<SubscriptionAssignment> stream(
      SubscriptionAssignmentView state, WorkloadConstraints constraints) {
    AvailableWork work = new AvailableWork(state, constraints);
    return StreamSupport.stream(work, false);
  }
}
