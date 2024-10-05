package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

class ExponentiallyWeightedMovingAverage {

  private final Duration windowSize;

  private Instant previousUpdateTimestamp;
  private double currentAverage = 0d;

  ExponentiallyWeightedMovingAverage(Duration windowSize) {
    this.windowSize = windowSize;
  }

  double update(double sample, Instant now) {
    if (previousUpdateTimestamp == null) {
      currentAverage = sample;
    } else {
      // This calculation is done in the same way as the Linux load average is calculated.
      // See: https://www.helpsystems.com/resources/guides/unix-load-average-part-1-how-it-works
      Duration elapsed = Duration.between(previousUpdateTimestamp, now);
      long elapsedMillis = Math.max(TimeUnit.MILLISECONDS.convert(elapsed), 0);
      double alpha = 1.0 - Math.exp(-1.0 * ((double) elapsedMillis / windowSize.toMillis()));
      currentAverage = (sample * alpha) + (currentAverage * (1.0 - alpha));
    }
    previousUpdateTimestamp = now;
    return currentAverage;
  }
}
