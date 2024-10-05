package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import static java.util.Comparator.comparing;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class ConsumerNode {

  static Comparator<ConsumerNode> LIGHTEST_CONSUMER_FIRST = comparing(ConsumerNode::getWeight);

  private final String consumerId;
  private final ConsumerNodeLoad initialLoad;
  private final int maxSubscriptionsPerConsumer;
  private final Set<ConsumerTask> tasks = new HashSet<>();
  private Weight weight = Weight.ZERO;

  ConsumerNode(String consumerId, ConsumerNodeLoad initialLoad, int maxSubscriptionsPerConsumer) {
    this.consumerId = consumerId;
    this.initialLoad = initialLoad;
    this.maxSubscriptionsPerConsumer = maxSubscriptionsPerConsumer;
  }

  String getConsumerId() {
    return consumerId;
  }

  void assign(ConsumerTask task) {
    tasks.add(task);
    weight = weight.add(task.getWeight());
  }

  void unassign(ConsumerTask task) {
    tasks.remove(task);
    weight = weight.subtract(task.getWeight());
  }

  void swap(ConsumerTask moveOut, ConsumerTask moveIn) {
    unassign(moveOut);
    assign(moveIn);
  }

  Set<ConsumerTask> getAssignedTasks() {
    return tasks;
  }

  int getAssignedTaskCount() {
    return tasks.size();
  }

  boolean isNotAssigned(ConsumerTask consumerTask) {
    return !tasks.contains(consumerTask);
  }

  boolean isFull() {
    return tasks.size() >= maxSubscriptionsPerConsumer;
  }

  Weight getWeight() {
    return weight;
  }

  ConsumerNodeLoad getInitialLoad() {
    return initialLoad;
  }

  @Override
  public String toString() {
    return consumerId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsumerNode that = (ConsumerNode) o;
    return Objects.equals(consumerId, that.consumerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(consumerId);
  }
}
