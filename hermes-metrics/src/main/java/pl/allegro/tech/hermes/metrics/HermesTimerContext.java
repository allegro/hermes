package pl.allegro.tech.hermes.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Timer;
import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class HermesTimerContext implements Closeable {

  private final Timer micrometerTimer;
  private final Clock clock;
  private final long startNanos;

  private HermesTimerContext(Timer micrometerTimer, Clock clock) {
    this.micrometerTimer = micrometerTimer;
    this.clock = clock;
    this.startNanos = clock.monotonicTime();
  }

  public static HermesTimerContext from(Timer micrometerTimer) {
    return new HermesTimerContext(micrometerTimer, Clock.SYSTEM);
  }

  @Override
  public void close() {
    reportTimer();
  }

  public Duration closeAndGet() {
    return Duration.ofNanos(reportTimer());
  }

  private long reportTimer() {
    long amount = clock.monotonicTime() - startNanos;
    micrometerTimer.record(amount, TimeUnit.NANOSECONDS);
    return amount;
  }
}
