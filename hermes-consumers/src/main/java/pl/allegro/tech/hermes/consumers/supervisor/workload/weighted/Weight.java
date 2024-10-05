package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

class Weight implements Comparable<Weight> {

  static final Weight ZERO = new Weight(0d);

  private final double operationsPerSecond;

  Weight(double operationsPerSecond) {
    checkArgument(
        operationsPerSecond >= 0d, "operationsPerSecond should be greater than or equal to zero");
    this.operationsPerSecond = operationsPerSecond;
  }

  static double calculatePercentageChange(Weight initialWeight, Weight finalWeight) {
    checkArgument(
        initialWeight.operationsPerSecond > 0d,
        "initialWeight.operationsPerSecond should be greater than zero");
    double delta = Math.abs(finalWeight.operationsPerSecond - initialWeight.operationsPerSecond);
    return (delta / initialWeight.operationsPerSecond) * 100;
  }

  static Weight max(Weight first, Weight second) {
    if (first.operationsPerSecond > second.operationsPerSecond) {
      return first;
    }
    return second;
  }

  Weight add(Weight addend) {
    return new Weight(operationsPerSecond + addend.operationsPerSecond);
  }

  Weight subtract(Weight subtrahend) {
    return new Weight(Math.max(operationsPerSecond - subtrahend.operationsPerSecond, 0d));
  }

  Weight multiply(double multiplicand) {
    return new Weight(operationsPerSecond * multiplicand);
  }

  Weight divide(int divisor) {
    return new Weight(operationsPerSecond / divisor);
  }

  boolean isGreaterThan(Weight other) {
    return operationsPerSecond > other.operationsPerSecond;
  }

  boolean isGreaterThanOrEqualTo(Weight other) {
    return isGreaterThan(other) || isEqualTo(other);
  }

  boolean isLessThan(Weight other) {
    return operationsPerSecond < other.operationsPerSecond;
  }

  boolean isEqualTo(Weight other) {
    return Double.compare(operationsPerSecond, other.operationsPerSecond) == 0;
  }

  double getOperationsPerSecond() {
    return operationsPerSecond;
  }

  @Override
  public int compareTo(Weight o) {
    return Double.compare(operationsPerSecond, o.operationsPerSecond);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Weight weight = (Weight) o;
    return Double.compare(weight.operationsPerSecond, operationsPerSecond) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(operationsPerSecond);
  }

  @Override
  public String toString() {
    return "(ops=" + operationsPerSecond + ")";
  }
}
