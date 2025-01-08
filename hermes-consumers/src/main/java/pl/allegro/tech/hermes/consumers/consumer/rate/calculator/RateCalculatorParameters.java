package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import java.time.Duration;

public interface RateCalculatorParameters {

  Duration getLimiterHeartbeatModeDelay();

  Duration getLimiterSlowModeDelay();

  double getConvergenceFactor();

  double getFailuresNoChangeToleranceRatio();

  double getFailuresSpeedUpToleranceRatio();
}
