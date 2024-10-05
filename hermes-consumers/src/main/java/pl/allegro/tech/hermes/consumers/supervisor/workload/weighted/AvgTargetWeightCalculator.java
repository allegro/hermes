package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Map;

public class AvgTargetWeightCalculator implements TargetWeightCalculator {

  private final WeightedWorkloadMetricsReporter metrics;

  public AvgTargetWeightCalculator(WeightedWorkloadMetricsReporter metrics) {
    this.metrics = metrics;
  }

  @Override
  public Map<String, Weight> calculate(Collection<ConsumerNode> consumers) {
    if (consumers.isEmpty()) {
      return Map.of();
    }

    metrics.reportCurrentWeights(consumers);
    Weight sum = consumers.stream().map(ConsumerNode::getWeight).reduce(Weight.ZERO, Weight::add);
    Weight average = sum.divide(consumers.size());

    Map<String, Weight> newWeights =
        consumers.stream().collect(toMap(ConsumerNode::getConsumerId, ignore -> average));
    metrics.reportProposedWeights(newWeights);
    return newWeights;
  }
}
