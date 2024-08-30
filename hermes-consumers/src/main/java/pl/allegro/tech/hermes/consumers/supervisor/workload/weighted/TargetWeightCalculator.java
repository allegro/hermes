package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import java.util.Collection;
import java.util.Map;

public interface TargetWeightCalculator {

  Map<String, Weight> calculate(Collection<ConsumerNode> consumers);
}
