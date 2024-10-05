package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.ConsumerNode.LIGHTEST_CONSUMER_FIRST;
import static pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.ConsumerTask.HEAVIEST_TASK_FIRST;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignment;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentView;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkBalancer;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkBalancingResult;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkloadConstraints;

public class WeightedWorkBalancer implements WorkBalancer {

  private static final Logger logger = LoggerFactory.getLogger(WeightedWorkBalancer.class);

  private final Clock clock;
  private final Duration stabilizationWindowSize;
  private final double minSignificantChangePercent;
  private final CurrentLoadProvider currentLoadProvider;
  private final TargetWeightCalculator targetWeightCalculator;

  public WeightedWorkBalancer(
      Clock clock,
      Duration stabilizationWindowSize,
      double minSignificantChangePercent,
      CurrentLoadProvider currentLoadProvider,
      TargetWeightCalculator targetWeightCalculator) {
    this.clock = clock;
    this.stabilizationWindowSize = stabilizationWindowSize;
    this.minSignificantChangePercent = minSignificantChangePercent;
    this.currentLoadProvider = currentLoadProvider;
    this.targetWeightCalculator = targetWeightCalculator;
  }

  @Override
  public WorkBalancingResult balance(
      List<SubscriptionName> subscriptions,
      List<String> activeConsumerNodes,
      SubscriptionAssignmentView currentState,
      WorkloadConstraints constraints) {
    AssignmentPlan initialPlan =
        restoreValidAssignments(subscriptions, activeConsumerNodes, currentState, constraints);
    AssignmentPlan planWithAllPossibleAssignments = tryAssignUnassignedTasks(initialPlan);
    AssignmentPlan finalPlan = rebalance(planWithAllPossibleAssignments);
    return finalPlan.toWorkBalancingResult();
  }

  private AssignmentPlan restoreValidAssignments(
      List<SubscriptionName> subscriptions,
      List<String> activeConsumerNodes,
      SubscriptionAssignmentView currentState,
      WorkloadConstraints constraints) {
    Map<String, ConsumerNode> consumers = createConsumers(activeConsumerNodes, constraints);
    List<ConsumerTask> unassignedTasks = new ArrayList<>();
    SubscriptionProfiles profiles = currentLoadProvider.getProfiles();
    for (SubscriptionName subscriptionName : subscriptions) {
      SubscriptionProfile subscriptionProfile = profiles.getProfile(subscriptionName);
      Queue<ConsumerTask> consumerTasks =
          createConsumerTasks(subscriptionName, subscriptionProfile, constraints);
      Set<String> consumerNodesForSubscription =
          currentState.getConsumerNodesForSubscription(subscriptionName);
      for (String consumerId : consumerNodesForSubscription) {
        ConsumerNode consumerNode = consumers.get(consumerId);
        if (consumerNode != null && !consumerTasks.isEmpty()) {
          ConsumerTask consumerTask = consumerTasks.poll();
          consumerNode.assign(consumerTask);
        }
      }
      unassignedTasks.addAll(consumerTasks);
    }
    return new AssignmentPlan(unassignedTasks, consumers.values());
  }

  private Map<String, ConsumerNode> createConsumers(
      List<String> activeConsumerNodes, WorkloadConstraints constraints) {
    return activeConsumerNodes.stream()
        .map(
            consumerId -> {
              ConsumerNodeLoad consumerNodeLoad =
                  currentLoadProvider.getConsumerNodeLoad(consumerId);
              return new ConsumerNode(
                  consumerId, consumerNodeLoad, constraints.getMaxSubscriptionsPerConsumer());
            })
        .collect(toMap(ConsumerNode::getConsumerId, Function.identity()));
  }

  private Queue<ConsumerTask> createConsumerTasks(
      SubscriptionName subscriptionName,
      SubscriptionProfile subscriptionProfile,
      WorkloadConstraints constraints) {
    int consumerCount = constraints.getConsumerCount(subscriptionName);
    return IntStream.range(0, consumerCount)
        .mapToObj(ignore -> new ConsumerTask(subscriptionName, subscriptionProfile))
        .collect(toCollection(ArrayDeque::new));
  }

  private AssignmentPlan tryAssignUnassignedTasks(AssignmentPlan assignmentPlan) {
    PriorityQueue<ConsumerTask> tasksToAssign = new PriorityQueue<>(HEAVIEST_TASK_FIRST);
    tasksToAssign.addAll(assignmentPlan.getUnassignedTasks());
    List<ConsumerTask> unassignedTasks = new ArrayList<>();
    while (!tasksToAssign.isEmpty()) {
      ConsumerTask consumerTask = tasksToAssign.poll();
      Optional<ConsumerNode> candidate =
          selectConsumerNode(consumerTask, assignmentPlan.getConsumers());
      if (candidate.isPresent()) {
        candidate.get().assign(consumerTask);
      } else {
        unassignedTasks.add(consumerTask);
      }
    }
    return new AssignmentPlan(unassignedTasks, assignmentPlan.getConsumers());
  }

  private Optional<ConsumerNode> selectConsumerNode(
      ConsumerTask consumerTask, Collection<ConsumerNode> consumers) {
    return consumers.stream()
        .filter(consumerNode -> consumerNode.isNotAssigned(consumerTask))
        .filter(consumerNode -> !consumerNode.isFull())
        .min(LIGHTEST_CONSUMER_FIRST);
  }

  private AssignmentPlan rebalance(AssignmentPlan plan) {
    Collection<ConsumerNode> consumers = plan.getConsumers();
    TargetConsumerLoad targetLoad = calculateTargetConsumerLoad(consumers);
    List<ConsumerNode> overloadedConsumers =
        consumers.stream()
            .filter(consumerNode -> isOverloaded(consumerNode, targetLoad))
            .sorted(new MostOverloadedConsumerFirst(targetLoad))
            .collect(toList());
    for (ConsumerNode overloaded : overloadedConsumers) {
      List<ConsumerNode> candidates =
          consumers.stream().filter(consumer -> !consumer.equals(overloaded)).collect(toList());
      for (ConsumerNode candidate : candidates) {
        tryMoveOutTasks(overloaded, candidate, targetLoad);
        trySwapTasks(overloaded, candidate, targetLoad);
        if (isBalanced(overloaded, targetLoad)) {
          break;
        }
      }
    }
    return new AssignmentPlan(plan.getUnassignedTasks(), consumers);
  }

  private TargetConsumerLoad calculateTargetConsumerLoad(Collection<ConsumerNode> consumers) {
    int totalNumberOfTasks = consumers.stream().mapToInt(ConsumerNode::getAssignedTaskCount).sum();
    int consumerCount = consumers.size();
    int maxNumberOfTasksPerConsumer =
        consumerCount == 0 ? 0 : divideWithRoundingUp(totalNumberOfTasks, consumerCount);
    Map<String, Weight> targetWeights = targetWeightCalculator.calculate(consumers);
    return new TargetConsumerLoad(targetWeights, maxNumberOfTasksPerConsumer);
  }

  private int divideWithRoundingUp(int dividend, int divisor) {
    return (dividend / divisor) + (dividend % divisor > 0 ? 1 : 0);
  }

  private void tryMoveOutTasks(
      ConsumerNode overloaded, ConsumerNode candidate, TargetConsumerLoad targetLoad) {
    List<ConsumerTask> candidatesToMoveOut =
        overloaded.getAssignedTasks().stream().filter(candidate::isNotAssigned).collect(toList());
    ListIterator<ConsumerTask> consumerTaskIterator = candidatesToMoveOut.listIterator();
    while (consumerTaskIterator.hasNext()
        && hasTooManyTasks(overloaded, targetLoad)
        && !hasTooManyTasks(candidate, targetLoad)) {
      ConsumerTask taskFromOverloaded = consumerTaskIterator.next();
      MoveOutProposal proposal = new MoveOutProposal(overloaded, candidate, taskFromOverloaded);
      if (isProfitable(proposal, targetLoad)) {
        overloaded.unassign(taskFromOverloaded);
        candidate.assign(taskFromOverloaded);
      }
    }
  }

  private boolean hasTooManyTasks(
      ConsumerNode consumerNode, TargetConsumerLoad targetConsumerLoad) {
    return consumerNode.getAssignedTaskCount() >= targetConsumerLoad.getNumberOfTasks();
  }

  private boolean isProfitable(MoveOutProposal proposal, TargetConsumerLoad targetLoad) {
    Weight targetWeight = targetLoad.getWeightForConsumer(proposal.getCandidateId());
    if (targetWeight.isGreaterThanOrEqualTo(proposal.getFinalCandidateWeight())) {
      logger.debug("MoveOut proposal will be applied:\n{}", proposal);
      return true;
    }
    return false;
  }

  private boolean isProfitable(SwapProposal proposal, TargetConsumerLoad targetLoad) {
    Weight initialOverloadedWeight = proposal.getOverloadedWeight();
    Weight finalOverloadedWeight = proposal.getFinalOverloadedWeight();
    Weight finalCandidateWeight = proposal.getFinalCandidateWeight();
    Weight overloadedTargetWeight = targetLoad.getWeightForConsumer(proposal.getOverloadedId());
    Weight candidateTargetWeight = targetLoad.getWeightForConsumer(proposal.getCandidateId());

    if (initialOverloadedWeight.isLessThan(overloadedTargetWeight)) {
      return false;
    }
    if (finalCandidateWeight.isGreaterThan(candidateTargetWeight)) {
      return false;
    }
    if (finalOverloadedWeight.isGreaterThan(initialOverloadedWeight)) {
      return false;
    }
    if (initialOverloadedWeight.isEqualTo(Weight.ZERO)) {
      return false;
    }
    double percentageChange =
        Weight.calculatePercentageChange(initialOverloadedWeight, finalOverloadedWeight);
    if (percentageChange >= minSignificantChangePercent) {
      logger.debug("Swap proposal will be applied:\n{}", proposal);
      return true;
    }
    return false;
  }

  private void trySwapTasks(
      ConsumerNode overloaded, ConsumerNode candidate, TargetConsumerLoad targetLoad) {
    List<ConsumerTask> tasksFromOverloaded = findTasksForMovingOut(overloaded, candidate);
    List<ConsumerTask> tasksFromCandidate = findTasksForMovingOut(candidate, overloaded);
    for (ConsumerTask taskFromOverloaded : tasksFromOverloaded) {
      ListIterator<ConsumerTask> tasksFromCandidateIterator = tasksFromCandidate.listIterator();
      while (tasksFromCandidateIterator.hasNext()) {
        ConsumerTask taskFromCandidate = tasksFromCandidateIterator.next();
        SwapProposal proposal =
            new SwapProposal(overloaded, candidate, taskFromOverloaded, taskFromCandidate);
        if (isProfitable(proposal, targetLoad)) {
          overloaded.swap(taskFromOverloaded, taskFromCandidate);
          candidate.swap(taskFromCandidate, taskFromOverloaded);
          tasksFromCandidateIterator.remove();
        }
        if (isBalanced(overloaded, targetLoad)) {
          return;
        }
      }
    }
  }

  private List<ConsumerTask> findTasksForMovingOut(ConsumerNode source, ConsumerNode destination) {
    return source.getAssignedTasks().stream()
        .filter(destination::isNotAssigned)
        .filter(this::isStable)
        .sorted(HEAVIEST_TASK_FIRST)
        .collect(toList());
  }

  private boolean isStable(ConsumerTask task) {
    return clock.instant().isAfter(task.getLastRebalanceTimestamp().plus(stabilizationWindowSize));
  }

  private boolean isOverloaded(ConsumerNode consumerNode, TargetConsumerLoad targetLoad) {
    Weight targetWeight = targetLoad.getWeightForConsumer(consumerNode.getConsumerId());
    return consumerNode.getWeight().isGreaterThan(targetWeight)
        || consumerNode.getAssignedTaskCount() > targetLoad.getNumberOfTasks();
  }

  private boolean isBalanced(ConsumerNode consumerNode, TargetConsumerLoad targetLoad) {
    Weight targetWeight = targetLoad.getWeightForConsumer(consumerNode.getConsumerId());
    return consumerNode.getWeight().isEqualTo(targetWeight)
        && consumerNode.getAssignedTaskCount() <= targetLoad.getNumberOfTasks();
  }

  private static class AssignmentPlan {

    private final List<ConsumerTask> unassignedTasks;
    private final Collection<ConsumerNode> consumers;

    AssignmentPlan(List<ConsumerTask> unassignedTasks, Collection<ConsumerNode> consumers) {
      this.unassignedTasks = unassignedTasks;
      this.consumers = consumers;
    }

    Collection<ConsumerNode> getConsumers() {
      return consumers;
    }

    List<ConsumerTask> getUnassignedTasks() {
      return unassignedTasks;
    }

    WorkBalancingResult toWorkBalancingResult() {
      Map<SubscriptionName, Set<SubscriptionAssignment>> targetView = new HashMap<>();
      for (ConsumerNode consumer : consumers) {
        for (ConsumerTask consumerTask : consumer.getAssignedTasks()) {
          SubscriptionName subscriptionName = consumerTask.getSubscriptionName();
          Set<SubscriptionAssignment> assignments =
              targetView.computeIfAbsent(subscriptionName, ignore -> new HashSet<>());
          assignments.add(new SubscriptionAssignment(consumer.getConsumerId(), subscriptionName));
        }
      }
      return new WorkBalancingResult(
          new SubscriptionAssignmentView(targetView), unassignedTasks.size());
    }
  }

  private static class MoveOutProposal {

    private final ConsumerNode overloaded;
    private final ConsumerNode candidate;
    private final Weight finalOverloadedWeight;
    private final Weight finalCandidateWeight;

    MoveOutProposal(
        ConsumerNode overloaded, ConsumerNode candidate, ConsumerTask taskFromOverloaded) {
      this.overloaded = overloaded;
      this.candidate = candidate;
      this.finalOverloadedWeight = overloaded.getWeight().subtract(taskFromOverloaded.getWeight());
      this.finalCandidateWeight = candidate.getWeight().add(taskFromOverloaded.getWeight());
    }

    Weight getFinalCandidateWeight() {
      return finalCandidateWeight;
    }

    String getCandidateId() {
      return candidate.getConsumerId();
    }

    @Override
    public String toString() {
      return toString(overloaded, finalOverloadedWeight)
          + "\n"
          + toString(candidate, finalCandidateWeight);
    }

    private String toString(ConsumerNode consumerNode, Weight newWeight) {
      return consumerNode
          + " (old weight = "
          + consumerNode.getWeight()
          + ", new weight = "
          + newWeight
          + ")";
    }
  }

  private static class SwapProposal {

    private final ConsumerNode overloaded;
    private final ConsumerNode candidate;
    private final Weight finalOverloadedWeight;
    private final Weight finalCandidateWeight;

    SwapProposal(
        ConsumerNode overloaded,
        ConsumerNode candidate,
        ConsumerTask taskFromOverloaded,
        ConsumerTask taskFromCandidate) {
      this.overloaded = overloaded;
      this.candidate = candidate;
      this.finalOverloadedWeight =
          overloaded
              .getWeight()
              .add(taskFromCandidate.getWeight())
              .subtract(taskFromOverloaded.getWeight());
      this.finalCandidateWeight =
          candidate
              .getWeight()
              .add(taskFromOverloaded.getWeight())
              .subtract(taskFromCandidate.getWeight());
    }

    Weight getOverloadedWeight() {
      return overloaded.getWeight();
    }

    Weight getFinalOverloadedWeight() {
      return finalOverloadedWeight;
    }

    Weight getFinalCandidateWeight() {
      return finalCandidateWeight;
    }

    String getCandidateId() {
      return candidate.getConsumerId();
    }

    String getOverloadedId() {
      return overloaded.getConsumerId();
    }

    @Override
    public String toString() {
      return toString(overloaded, finalOverloadedWeight)
          + "\n"
          + toString(candidate, finalCandidateWeight);
    }

    private String toString(ConsumerNode consumerNode, Weight newWeight) {
      return consumerNode
          + " (old weight = "
          + consumerNode.getWeight()
          + ", new weight = "
          + newWeight
          + ")";
    }
  }

  private static class TargetConsumerLoad {

    private final Map<String, Weight> weights;
    private final int numberOfTasks;

    TargetConsumerLoad(Map<String, Weight> weights, int numberOfTasks) {
      this.weights = weights;
      this.numberOfTasks = numberOfTasks;
    }

    Weight getWeightForConsumer(String consumerId) {
      return weights.get(consumerId);
    }

    int getNumberOfTasks() {
      return numberOfTasks;
    }
  }

  private static class MostOverloadedConsumerFirst implements Comparator<ConsumerNode> {

    private final TargetConsumerLoad targetLoad;

    MostOverloadedConsumerFirst(TargetConsumerLoad targetLoad) {
      this.targetLoad = targetLoad;
    }

    @Override
    public int compare(ConsumerNode first, ConsumerNode second) {
      Weight firstTargetLoad = targetLoad.getWeightForConsumer(first.getConsumerId());
      Weight secondTargetLoad = targetLoad.getWeightForConsumer(second.getConsumerId());
      Weight firstError = first.getWeight().subtract(firstTargetLoad);
      Weight secondError = second.getWeight().subtract(secondTargetLoad);
      return secondError.compareTo(firstError);
    }
  }
}
