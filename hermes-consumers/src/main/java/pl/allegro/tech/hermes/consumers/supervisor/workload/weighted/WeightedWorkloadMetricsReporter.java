package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.metric.WorkloadMetrics;

public class WeightedWorkloadMetricsReporter {

  private final WorkloadMetrics metrics;
  private final Map<String, Double> currentWeights = new ConcurrentHashMap<>();
  private final Map<String, Double> proposedWeights = new ConcurrentHashMap<>();
  private final Map<String, Double> currentScores = new ConcurrentHashMap<>();
  private final Map<String, Double> proposedScores = new ConcurrentHashMap<>();
  private final Map<String, Double> scoringErrors = new ConcurrentHashMap<>();

  public WeightedWorkloadMetricsReporter(MetricsFacade metrics) {
    this.metrics = metrics.workload();
  }

  void reportCurrentScore(String consumerId, double score) {
    if (!currentScores.containsKey(consumerId)) {
      metrics.registerCurrentScoreGauge(
          consumerId, currentScores, scores -> scores.getOrDefault(consumerId, 0d));
    }
    currentScores.put(consumerId, score);
  }

  void reportProposedScore(String consumerId, double score) {
    if (!currentScores.containsKey(consumerId)) {
      metrics.registerProposedErrorGauge(
          consumerId, proposedScores, scores -> scores.getOrDefault(consumerId, 0d));
    }
    proposedScores.put(consumerId, score);
  }

  void reportScoringError(String consumerId, double error) {
    if (!scoringErrors.containsKey(consumerId)) {
      metrics.registerScoringErrorGauge(
          consumerId, scoringErrors, errors -> errors.getOrDefault(consumerId, 0d));
    }
    scoringErrors.put(consumerId, error);
  }

  void reportCurrentWeights(Collection<ConsumerNode> consumers) {
    for (ConsumerNode consumerNode : consumers) {
      String consumerId = consumerNode.getConsumerId();
      if (!currentWeights.containsKey(consumerId)) {
        metrics.registerCurrentWeightGauge(
            consumerId, currentWeights, weights -> weights.getOrDefault(consumerId, 0d));
      }
      currentWeights.put(consumerId, consumerNode.getWeight().getOperationsPerSecond());
    }
  }

  void reportProposedWeights(Map<String, Weight> newWeights) {
    for (Map.Entry<String, Weight> entry : newWeights.entrySet()) {
      String consumerId = entry.getKey();
      if (!proposedWeights.containsKey(consumerId)) {
        metrics.registerProposedWeightGauge(
            consumerId, proposedWeights, weights -> weights.getOrDefault(consumerId, 0d));
      }
      proposedWeights.put(consumerId, entry.getValue().getOperationsPerSecond());
    }
  }

  void unregisterLeaderMetrics() {
    unregisterMetricsForConsumersOtherThan(emptySet());
  }

  void unregisterMetricsForConsumersOtherThan(Set<String> consumerIds) {
    metrics.unregisterAllWorkloadWeightedGaugesForConsumerIds(findConsumerIdsToRemove(consumerIds));
  }

  private Set<String> findConsumerIdsToRemove(Set<String> activeIds) {
    return Sets.newHashSet(
            Iterables.concat(
                currentScores.keySet(),
                proposedScores.keySet(),
                scoringErrors.keySet(),
                currentWeights.keySet(),
                proposedWeights.keySet()))
        .stream()
        .filter(consumerId -> !activeIds.contains(consumerId))
        .collect(toSet());
  }
}
