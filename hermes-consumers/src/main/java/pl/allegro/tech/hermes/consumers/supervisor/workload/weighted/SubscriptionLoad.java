package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import java.util.Objects;

class SubscriptionLoad {

  private final double operationsPerSecond;

  SubscriptionLoad(double operationsPerSecond) {
    this.operationsPerSecond = operationsPerSecond;
  }

  double getOperationsPerSecond() {
    return operationsPerSecond;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubscriptionLoad that = (SubscriptionLoad) o;
    return Double.compare(that.operationsPerSecond, operationsPerSecond) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(operationsPerSecond);
  }
}
