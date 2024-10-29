package pl.allegro.tech.hermes.consumers.supervisor.workload;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import pl.allegro.tech.hermes.api.SubscriptionName;

public class SubscriptionAssignmentView {

  private final Map<SubscriptionName, Set<SubscriptionAssignment>> subscriptionAssignments;
  private final Map<String, Set<SubscriptionAssignment>> consumerNodeAssignments;

  public SubscriptionAssignmentView(Map<SubscriptionName, Set<SubscriptionAssignment>> view) {
    this.subscriptionAssignments = setupSubscriptionAssignments(view);
    this.consumerNodeAssignments = setupConsumerNodeAssignments(view);
  }

  private Map<SubscriptionName, Set<SubscriptionAssignment>> setupSubscriptionAssignments(
      Map<SubscriptionName, Set<SubscriptionAssignment>> view) {
    Map<SubscriptionName, Set<SubscriptionAssignment>> map = new HashMap<>();
    view.entrySet().forEach(entry -> map.put(entry.getKey(), new HashSet<>(entry.getValue())));
    return map;
  }

  private Map<String, Set<SubscriptionAssignment>> setupConsumerNodeAssignments(
      Map<SubscriptionName, Set<SubscriptionAssignment>> view) {
    Map<String, Set<SubscriptionAssignment>> map = new HashMap<>();
    view.values().stream()
        .flatMap(Set::stream)
        .forEach(
            assignment -> {
              if (!map.containsKey(assignment.getConsumerNodeId())) {
                map.put(assignment.getConsumerNodeId(), new HashSet<>());
              }
              map.get(assignment.getConsumerNodeId()).add(assignment);
            });
    return map;
  }

  public Set<SubscriptionName> getSubscriptions() {
    return subscriptionAssignments.keySet();
  }

  public int getSubscriptionsCount() {
    return subscriptionAssignments.size();
  }

  public Set<String> getConsumerNodes() {
    return consumerNodeAssignments.keySet();
  }

  public List<SubscriptionAssignment> getAllAssignments() {
    return subscriptionAssignments.values().stream().flatMap(Set::stream).collect(toList());
  }

  public Set<String> getConsumerNodesForSubscription(SubscriptionName subscriptionName) {
    return getAssignmentsForSubscription(subscriptionName).stream()
        .map(SubscriptionAssignment::getConsumerNodeId)
        .collect(toSet());
  }

  public Set<SubscriptionAssignment> getAssignmentsForSubscription(
      SubscriptionName subscriptionName) {
    return Collections.unmodifiableSet(
        subscriptionAssignments.getOrDefault(subscriptionName, Collections.emptySet()));
  }

  public Set<SubscriptionName> getSubscriptionsForConsumerNode(String nodeId) {
    return getAssignmentsForConsumerNode(nodeId).stream()
        .map(SubscriptionAssignment::getSubscriptionName)
        .collect(toSet());
  }

  public Set<SubscriptionAssignment> getAssignmentsForConsumerNode(String nodeId) {
    return Collections.unmodifiableSet(
        consumerNodeAssignments.getOrDefault(nodeId, Collections.emptySet()));
  }

  private void removeSubscription(SubscriptionName subscription) {
    consumerNodeAssignments
        .values()
        .forEach(
            assignments ->
                assignments.removeIf(
                    assignment -> assignment.getSubscriptionName().equals(subscription)));
    subscriptionAssignments.remove(subscription);
  }

  private void removeConsumerNode(String nodeId) {
    subscriptionAssignments
        .values()
        .forEach(
            assignments ->
                assignments.removeIf(assignment -> assignment.getConsumerNodeId().equals(nodeId)));
    consumerNodeAssignments.remove(nodeId);
  }

  private void addSubscription(SubscriptionName subscriptionName) {
    subscriptionAssignments.putIfAbsent(subscriptionName, new HashSet<>());
  }

  private void addConsumerNode(String nodeId) {
    consumerNodeAssignments.putIfAbsent(nodeId, new HashSet<>());
  }

  private void addAssignment(SubscriptionAssignment assignment) {
    subscriptionAssignments.get(assignment.getSubscriptionName()).add(assignment);
    if (!consumerNodeAssignments.containsKey(assignment.getConsumerNodeId())) {
      addConsumerNode(assignment.getConsumerNodeId());
    }
    consumerNodeAssignments.get(assignment.getConsumerNodeId()).add(assignment);
  }

  private void removeAssignment(SubscriptionAssignment assignment) {
    subscriptionAssignments.get(assignment.getSubscriptionName()).remove(assignment);
    consumerNodeAssignments.get(assignment.getConsumerNodeId()).remove(assignment);
  }

  private void transferAssignment(String from, String to, SubscriptionName subscriptionName) {
    removeAssignment(new SubscriptionAssignment(from, subscriptionName));
    addAssignment(new SubscriptionAssignment(to, subscriptionName));
  }

  public SubscriptionAssignmentView deletions(SubscriptionAssignmentView target) {
    return difference(this, target);
  }

  public SubscriptionAssignmentView additions(SubscriptionAssignmentView target) {
    return difference(target, this);
  }

  private static SubscriptionAssignmentView difference(
      SubscriptionAssignmentView first, SubscriptionAssignmentView second) {
    HashMap<SubscriptionName, Set<SubscriptionAssignment>> result = new HashMap<>();
    for (SubscriptionName subscription : first.getSubscriptions()) {
      Set<SubscriptionAssignment> assignments = first.getAssignmentsForSubscription(subscription);
      if (!second.getSubscriptions().contains(subscription)) {
        result.put(subscription, assignments);
      } else {
        Sets.SetView<SubscriptionAssignment> difference =
            Sets.difference(assignments, second.getAssignmentsForSubscription(subscription));
        if (!difference.isEmpty()) {
          result.put(subscription, difference);
        }
      }
    }
    return new SubscriptionAssignmentView(result);
  }

  public static SubscriptionAssignmentView of(Set<SubscriptionAssignment> assignments) {
    Map<SubscriptionName, Set<SubscriptionAssignment>> snapshot = new HashMap<>();
    for (SubscriptionAssignment assignment : assignments) {
      snapshot.compute(
          assignment.getSubscriptionName(),
          (k, v) -> {
            v = (v == null ? new HashSet<>() : v);
            v.add(assignment);
            return v;
          });
    }
    return new SubscriptionAssignmentView(snapshot);
  }

  public static SubscriptionAssignmentView copyOf(SubscriptionAssignmentView currentState) {
    return new SubscriptionAssignmentView(currentState.subscriptionAssignments);
  }

  public int getAssignmentsCountForSubscription(SubscriptionName subscription) {
    return subscriptionAssignments.get(subscription).size();
  }

  public int getAssignmentsCountForConsumerNode(String nodeId) {
    return consumerNodeAssignments.get(nodeId).size();
  }

  @Override
  public int hashCode() {
    return Objects.hash(subscriptionAssignments);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubscriptionAssignmentView that = (SubscriptionAssignmentView) o;
    return Objects.equals(subscriptionAssignments, that.subscriptionAssignments);
  }

  public interface Transformer {
    void removeSubscription(SubscriptionName subscriptionName);

    void removeConsumerNode(String nodeId);

    void addSubscription(SubscriptionName subscriptionName);

    void addConsumerNode(String nodeId);

    void addAssignment(SubscriptionAssignment assignment);

    void removeAssignment(SubscriptionAssignment assignment);

    void transferAssignment(String from, String to, SubscriptionName subscriptionName);
  }

  public SubscriptionAssignmentView transform(
      BiConsumer<SubscriptionAssignmentView, Transformer> consumer) {
    SubscriptionAssignmentView view = SubscriptionAssignmentView.copyOf(this);
    consumer.accept(
        view,
        new Transformer() {
          @Override
          public void removeSubscription(SubscriptionName subscriptionName) {
            view.removeSubscription(subscriptionName);
          }

          @Override
          public void removeConsumerNode(String nodeId) {
            view.removeConsumerNode(nodeId);
          }

          @Override
          public void addSubscription(SubscriptionName subscriptionName) {
            view.addSubscription(subscriptionName);
          }

          @Override
          public void addConsumerNode(String nodeId) {
            view.addConsumerNode(nodeId);
          }

          @Override
          public void addAssignment(SubscriptionAssignment assignment) {
            view.addAssignment(assignment);
          }

          @Override
          public void removeAssignment(SubscriptionAssignment assignment) {
            view.removeAssignment(assignment);
          }

          @Override
          public void transferAssignment(
              String from, String to, SubscriptionName subscriptionName) {
            view.transferAssignment(from, to, subscriptionName);
          }
        });
    return copyOf(view);
  }
}
