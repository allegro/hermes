package pl.allegro.tech.hermes.consumers.consumer.idletime;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.min;

public class ExponentiallyGrowingIdleTimeCalculator implements IdleTimeCalculator {

  private final long base;
  private final long initialIdleTimeMs;
  private final long maxIdleTimeMs;
  private long currentIdleTimeMs;

  public ExponentiallyGrowingIdleTimeCalculator(long initialIdleTimeMs, long maxIdleTimeMs) {
    this(2, initialIdleTimeMs, maxIdleTimeMs);
  }

  public ExponentiallyGrowingIdleTimeCalculator(
      long base, long initialIdleTime, long maxIdleTimeMs) {
    checkArgument(base > 0, "base should be greater than zero");
    checkArgument(initialIdleTime > 0, "initialIdleTimeMs should be greater than zero");
    checkArgument(maxIdleTimeMs > 0, "maxIdleTimeMs should be greater than zero");
    checkArgument(
        initialIdleTime <= maxIdleTimeMs,
        "maxIdleTimeMs should be grater or equal initialIdleTimeMs");

    this.base = base;
    this.initialIdleTimeMs = initialIdleTime;
    this.maxIdleTimeMs = maxIdleTimeMs;
    this.currentIdleTimeMs = initialIdleTime;
  }

  @Override
  public long increaseIdleTime() {
    long previousIdleTime = this.currentIdleTimeMs;
    this.currentIdleTimeMs = min(this.currentIdleTimeMs * base, maxIdleTimeMs);
    return previousIdleTime;
  }

  @Override
  public long getIdleTime() {
    return currentIdleTimeMs;
  }

  @Override
  public void reset() {
    this.currentIdleTimeMs = initialIdleTimeMs;
  }
}
