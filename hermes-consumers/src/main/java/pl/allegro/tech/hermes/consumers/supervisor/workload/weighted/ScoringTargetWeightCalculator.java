package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScoringTargetWeightCalculator implements TargetWeightCalculator {

  private static final double MIN_SCORE = 0.01d;
  private static final double MAX_SCORE = 1.0d;

  private final WeightedWorkloadMetricsReporter metrics;
  private final Clock clock;
  private final Duration scoringWindowSize;
  private final double scoringGain;
  private final Map<String, ExponentiallyWeightedMovingAverage> scores = new HashMap<>();

  public ScoringTargetWeightCalculator(
      WeightedWorkloadMetricsReporter metrics,
      Clock clock,
      Duration scoringWindowSize,
      double scoringGain) {
    this.metrics = metrics;
    this.clock = clock;
    this.scoringWindowSize = scoringWindowSize;
    this.scoringGain = scoringGain;
  }

  @Override
  public Map<String, Weight> calculate(Collection<ConsumerNode> consumers) {
    removeScoresForInactiveConsumers(consumers);

    metrics.reportCurrentWeights(consumers);
    Map<String, ConsumerNodeLoad> loadPerConsumer = mapConsumerIdToLoad(consumers);
    double targetCpuUtilization = calculateTargetCpuUtilization(loadPerConsumer);
    Map<String, Double> currentScores = calculateCurrentScores(loadPerConsumer);

    Map<String, Double> newScores = new HashMap<>();
    for (Map.Entry<String, Double> entry : currentScores.entrySet()) {
      String consumerId = entry.getKey();
      double cpuUtilization = loadPerConsumer.get(consumerId).getCpuUtilization();
      double error = targetCpuUtilization - cpuUtilization;
      double currentScore = entry.getValue();
      double newScore = calculateNewScore(consumerId, currentScore, error);
      newScores.put(consumerId, newScore);
      metrics.reportCurrentScore(consumerId, currentScore);
      metrics.reportProposedScore(consumerId, newScore);
      metrics.reportScoringError(consumerId, error);
    }

    Map<String, Weight> newWeights = calculateWeights(consumers, newScores);
    metrics.reportProposedWeights(newWeights);
    return newWeights;
  }

  private void removeScoresForInactiveConsumers(Collection<ConsumerNode> consumers) {
    Set<String> consumerIds = consumers.stream().map(ConsumerNode::getConsumerId).collect(toSet());
    scores.entrySet().removeIf(e -> !consumerIds.contains(e.getKey()));
  }

  private Map<String, ConsumerNodeLoad> mapConsumerIdToLoad(Collection<ConsumerNode> consumers) {
    return consumers.stream()
        .collect(toMap(ConsumerNode::getConsumerId, ConsumerNode::getInitialLoad));
  }

  private double calculateTargetCpuUtilization(Map<String, ConsumerNodeLoad> loadPerConsumer) {
    return loadPerConsumer.values().stream()
        .filter(ConsumerNodeLoad::isDefined)
        .mapToDouble(ConsumerNodeLoad::getCpuUtilization)
        .average()
        .orElse(0d);
  }

  private Map<String, Double> calculateCurrentScores(
      Map<String, ConsumerNodeLoad> loadPerConsumer) {
    Map<String, Double> opsPerConsumer =
        loadPerConsumer.entrySet().stream()
            .filter(e -> e.getValue().isDefined())
            .collect(toMap(Map.Entry::getKey, e -> e.getValue().sumOperationsPerSecond()));
    double opsSum = opsPerConsumer.values().stream().mapToDouble(ops -> ops).sum();
    return opsPerConsumer.entrySet().stream()
        .collect(toMap(Map.Entry::getKey, e -> calculateCurrentScore(e.getValue(), opsSum)));
  }

  private double calculateCurrentScore(double ops, double opsSum) {
    if (opsSum > 0) {
      return ops / opsSum;
    }
    return 0;
  }

  private double calculateNewScore(String consumerId, double currentScore, double error) {
    double rawScore = currentScore + scoringGain * error;
    ExponentiallyWeightedMovingAverage average =
        scores.computeIfAbsent(
            consumerId, ignore -> new ExponentiallyWeightedMovingAverage(scoringWindowSize));
    double avg = average.update(rawScore, clock.instant());
    return ensureScoreRanges(avg);
  }

  private double ensureScoreRanges(double score) {
    return Math.max(Math.min(score, MAX_SCORE), MIN_SCORE);
  }

  private Map<String, Weight> calculateWeights(
      Collection<ConsumerNode> consumers, Map<String, Double> newScores) {
    Weight sum = consumers.stream().map(ConsumerNode::getWeight).reduce(Weight.ZERO, Weight::add);
    Weight avgWeight = calculateAvgWeight(sum, consumers.size());
    List<ConsumerNode> consumersWithoutScore =
        consumers.stream()
            .filter(consumerNode -> !newScores.containsKey(consumerNode.getConsumerId()))
            .collect(toList());
    Map<String, Weight> newWeights = new HashMap<>();
    for (ConsumerNode consumerNode : consumersWithoutScore) {
      newWeights.put(consumerNode.getConsumerId(), avgWeight);
      sum = sum.subtract(avgWeight);
    }
    double newScoresSum = newScores.values().stream().mapToDouble(score -> score).sum();
    for (Map.Entry<String, Double> entry : newScores.entrySet()) {
      Weight weight = sum.multiply(entry.getValue() / newScoresSum);
      newWeights.put(entry.getKey(), weight);
    }
    return newWeights;
  }

  private Weight calculateAvgWeight(Weight sum, int consumerCount) {
    if (consumerCount == 0) {
      return Weight.ZERO;
    }
    return sum.divide(consumerCount);
  }
}
