package pl.allegro.tech.hermes.metrics;

import io.micrometer.core.instrument.Timer;

import java.time.Duration;

public class HermesTimer {
  private final Timer micrometerTimer;

  private HermesTimer(Timer micrometerTimer) {
    this.micrometerTimer = micrometerTimer;
  }

  public static HermesTimer from(io.micrometer.core.instrument.Timer micrometerTimer) {
    return new HermesTimer(micrometerTimer);
  }

  public HermesTimerContext time() {
    return HermesTimerContext.from(micrometerTimer);
  }

  public void record(Duration duration) {
    micrometerTimer.record(duration);
  }
}