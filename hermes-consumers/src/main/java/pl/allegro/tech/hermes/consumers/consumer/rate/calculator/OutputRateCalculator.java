package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import java.util.EnumMap;
import java.util.Map;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateProvider;

public class OutputRateCalculator {

  public enum Mode {
    NORMAL,
    SLOW,
    HEARTBEAT
  }

  private final Map<Mode, ModeOutputRateCalculator> modeCalculators = new EnumMap<>(Mode.class);

  private final MaxRateProvider maxRateProvider;

  public OutputRateCalculator(
      RateCalculatorParameters rateCalculatorParameters, MaxRateProvider maxRateProvider) {
    this.maxRateProvider = maxRateProvider;

    modeCalculators.put(
        Mode.NORMAL,
        new NormalModeOutputRateCalculator(
            rateCalculatorParameters.getConvergenceFactor(),
            1.0 / rateCalculatorParameters.getLimiterSlowModeDelay().toSeconds(),
            rateCalculatorParameters.getFailuresSpeedUpToleranceRatio(),
            rateCalculatorParameters.getFailuresNoChangeToleranceRatio()));
    modeCalculators.put(
        Mode.SLOW,
        new SlowModeOutputRateCalculator(
            1.0 / rateCalculatorParameters.getLimiterHeartbeatModeDelay().toSeconds()));
    modeCalculators.put(
        Mode.HEARTBEAT,
        new HeartbeatModeOutputRateCalculator(
            1.0 / rateCalculatorParameters.getLimiterSlowModeDelay().toSeconds()));
  }

  public OutputRateCalculationResult recalculateRate(
      SendCounters counters, Mode currentMode, double currentRateLimit) {

    double maximumRate = maxRateProvider.get();
    OutputRateCalculationResult recalculatedResult =
        modeCalculators
            .get(currentMode)
            .calculateOutputRate(currentRateLimit, maximumRate, counters);

    if (recalculatedResult.rate() > maximumRate) {
      recalculatedResult = OutputRateCalculationResult.adjustRate(recalculatedResult, maximumRate);
    }

    return recalculatedResult;
  }

  public void updateSubscription(Subscription newSubscription) {
    maxRateProvider.updateSubscription(newSubscription);
  }

  public void start() {
    maxRateProvider.start();
  }

  public void shutdown() {
    maxRateProvider.shutdown();
  }
}
