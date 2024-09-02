package pl.allegro.tech.hermes.consumers.supervisor.workload.selective;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignment;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentView;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkBalancer;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkBalancingResult;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkloadConstraints;

public class SelectiveWorkBalancer implements WorkBalancer {

  private static final Logger logger = LoggerFactory.getLogger(SelectiveWorkBalancer.class);

  @Override
  public WorkBalancingResult balance(
      List<SubscriptionName> subscriptions,
      List<String> activeConsumerNodes,
      SubscriptionAssignmentView currentState,
      WorkloadConstraints constraints) {

    List<SubscriptionName> removedSubscriptions =
        findRemovedSubscriptions(currentState, subscriptions);
    List<String> inactiveConsumers = findInactiveConsumers(currentState, activeConsumerNodes);
    List<SubscriptionName> newSubscriptions = findNewSubscriptions(currentState, subscriptions);
    List<String> newConsumers = findNewConsumers(currentState, activeConsumerNodes);

    SubscriptionAssignmentView balancedState =
        balance(
            currentState,
            removedSubscriptions,
            inactiveConsumers,
            newSubscriptions,
            newConsumers,
            constraints);

    log(subscriptions, activeConsumerNodes, currentState, balancedState);

    return new WorkBalancingResult(
        balancedState, countMissingResources(subscriptions, balancedState, constraints));
  }

  private SubscriptionAssignmentView balance(
      SubscriptionAssignmentView currentState,
      List<SubscriptionName> removedSubscriptions,
      List<String> inactiveConsumers,
      List<SubscriptionName> newSubscriptions,
      List<String> newConsumers,
      WorkloadConstraints constraints) {
    return currentState.transform(
        (state, transformer) -> {
          removedSubscriptions.forEach(transformer::removeSubscription);
          inactiveConsumers.forEach(transformer::removeConsumerNode);
          newSubscriptions.forEach(transformer::addSubscription);
          newConsumers.forEach(transformer::addConsumerNode);
          minimizeWorkload(state, transformer, constraints);
          AvailableWork.stream(state, constraints).forEach(transformer::addAssignment);
          equalizeWorkload(state, transformer);
        });
  }

  private void minimizeWorkload(
      SubscriptionAssignmentView state,
      SubscriptionAssignmentView.Transformer transformer,
      WorkloadConstraints workloadConstraints) {
    state.getSubscriptions().stream()
        .flatMap(
            subscriptionName ->
                findRedundantAssignments(state, subscriptionName, workloadConstraints))
        .forEach(transformer::removeAssignment);
  }

  private Stream<SubscriptionAssignment> findRedundantAssignments(
      SubscriptionAssignmentView state,
      SubscriptionName subscriptionName,
      WorkloadConstraints constraints) {
    final int assignedConsumers = state.getAssignmentsCountForSubscription(subscriptionName);
    final int requiredConsumers = constraints.getConsumerCount(subscriptionName);
    int redundantConsumers = assignedConsumers - requiredConsumers;
    if (redundantConsumers > 0) {
      Stream.Builder<SubscriptionAssignment> redundant = Stream.builder();
      Iterator<SubscriptionAssignment> iterator =
          state.getAssignmentsForSubscription(subscriptionName).iterator();
      while (redundantConsumers > 0 && iterator.hasNext()) {
        SubscriptionAssignment assignment = iterator.next();
        redundant.add(assignment);
        redundantConsumers--;
      }
      return redundant.build();
    }
    return Stream.empty();
  }

  private int countMissingResources(
      List<SubscriptionName> subscriptions,
      SubscriptionAssignmentView state,
      WorkloadConstraints constraints) {
    return subscriptions.stream()
        .mapToInt(
            s -> {
              int requiredConsumers = constraints.getConsumerCount(s);
              int subscriptionAssignments = state.getAssignmentsCountForSubscription(s);
              int missing = requiredConsumers - subscriptionAssignments;
              if (missing != 0) {
                logger.info(
                    "Subscription {} has {} != {} (default) assignments",
                    s,
                    subscriptionAssignments,
                    requiredConsumers);
              }
              return missing;
            })
        .sum();
  }

  private void equalizeWorkload(
      SubscriptionAssignmentView state, SubscriptionAssignmentView.Transformer transformer) {
    if (state.getSubscriptionsCount() > 1 && !state.getConsumerNodes().isEmpty()) {
      boolean transferred;
      do {
        transferred = false;

        String maxLoaded = maxLoadedConsumerNode(state);
        String minLoaded = minLoadedConsumerNode(state);
        int maxLoad = state.getAssignmentsCountForConsumerNode(maxLoaded);
        int minLoad = state.getAssignmentsCountForConsumerNode(minLoaded);

        while (maxLoad > minLoad + 1) {
          Optional<SubscriptionName> subscription =
              getSubscriptionForTransfer(state, maxLoaded, minLoaded);
          if (subscription.isPresent()) {
            transformer.transferAssignment(maxLoaded, minLoaded, subscription.get());
            transferred = true;
          } else {
            break;
          }
          maxLoad--;
          minLoad++;
        }
      } while (transferred);
    }
  }

  private String maxLoadedConsumerNode(SubscriptionAssignmentView state) {
    return state.getConsumerNodes().stream()
        .max(comparingInt(state::getAssignmentsCountForConsumerNode))
        .get();
  }

  private String minLoadedConsumerNode(SubscriptionAssignmentView state) {
    return state.getConsumerNodes().stream()
        .min(comparingInt(state::getAssignmentsCountForConsumerNode))
        .get();
  }

  private Optional<SubscriptionName> getSubscriptionForTransfer(
      SubscriptionAssignmentView state, String maxLoaded, String minLoaded) {
    return state.getSubscriptionsForConsumerNode(maxLoaded).stream()
        .filter(s -> !state.getConsumerNodesForSubscription(s).contains(minLoaded))
        .findAny();
  }

  private List<SubscriptionName> findRemovedSubscriptions(
      SubscriptionAssignmentView state, List<SubscriptionName> subscriptions) {
    return state.getSubscriptions().stream()
        .filter(s -> !subscriptions.contains(s))
        .collect(toList());
  }

  private List<String> findInactiveConsumers(
      SubscriptionAssignmentView state, List<String> activeConsumers) {
    return state.getConsumerNodes().stream()
        .filter(c -> !activeConsumers.contains(c))
        .collect(toList());
  }

  private List<SubscriptionName> findNewSubscriptions(
      SubscriptionAssignmentView state, List<SubscriptionName> subscriptions) {
    return subscriptions.stream()
        .filter(s -> !state.getSubscriptions().contains(s))
        .collect(toList());
  }

  private List<String> findNewConsumers(
      SubscriptionAssignmentView state, List<String> activeConsumers) {
    return activeConsumers.stream()
        .filter(c -> !state.getConsumerNodes().contains(c))
        .collect(toList());
  }

  private void log(
      List<SubscriptionName> subscriptions,
      List<String> activeConsumerNodes,
      SubscriptionAssignmentView currentState,
      SubscriptionAssignmentView balancedState) {
    logger.info(
        "Balancing {} subscriptions across {} nodes with previous {} assignments"
            + " produced {} assignments",
        subscriptions.size(),
        activeConsumerNodes.size(),
        currentState.getAllAssignments().size(),
        balancedState.getAllAssignments().size());
  }
}
