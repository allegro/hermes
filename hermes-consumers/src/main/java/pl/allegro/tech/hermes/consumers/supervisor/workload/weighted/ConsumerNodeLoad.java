package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import java.util.Map;
import pl.allegro.tech.hermes.api.SubscriptionName;

class ConsumerNodeLoad {

  static final ConsumerNodeLoad UNDEFINED = new ConsumerNodeLoad(-1d, Map.of());

  private final double cpuUtilization;
  private final Map<SubscriptionName, SubscriptionLoad> loadPerSubscription;

  ConsumerNodeLoad(
      double cpuUtilization, Map<SubscriptionName, SubscriptionLoad> loadPerSubscription) {
    this.cpuUtilization = cpuUtilization;
    this.loadPerSubscription = loadPerSubscription;
  }

  Map<SubscriptionName, SubscriptionLoad> getLoadPerSubscription() {
    return loadPerSubscription;
  }

  double getCpuUtilization() {
    return cpuUtilization;
  }

  double sumOperationsPerSecond() {
    return loadPerSubscription.values().stream()
        .mapToDouble(SubscriptionLoad::getOperationsPerSecond)
        .sum();
  }

  boolean isDefined() {
    return cpuUtilization != UNDEFINED.getCpuUtilization();
  }
}
