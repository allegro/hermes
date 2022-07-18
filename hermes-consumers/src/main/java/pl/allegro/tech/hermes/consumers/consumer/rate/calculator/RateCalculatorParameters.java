package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import java.time.Duration;

public class RateCalculatorParameters {

    private final Duration limiterHeartbeatModeDelay;

    private final Duration limiterSlowModeDelay;

    private final double convergenceFactor;

    private final double failuresNoChangeToleranceRatio;

    private final double failuresSpeedUpToleranceRatio;

    public Duration getLimiterHeartbeatModeDelay() {
        return limiterHeartbeatModeDelay;
    }

    public Duration getLimiterSlowModeDelay() {
        return limiterSlowModeDelay;
    }

    public double getConvergenceFactor() {
        return convergenceFactor;
    }

    public double getFailuresNoChangeToleranceRatio() {
        return failuresNoChangeToleranceRatio;
    }

    public double getFailuresSpeedUpToleranceRatio() {
        return failuresSpeedUpToleranceRatio;
    }

    public RateCalculatorParameters(Duration limiterHeartbeatModeDelay,
                                    Duration limiterSlowModeDelay,
                                    double convergenceFactor,
                                    double failuresNoChangeToleranceRatio,
                                    double failuresSpeedUpToleranceRatio) {
        this.limiterHeartbeatModeDelay = limiterHeartbeatModeDelay;
        this.limiterSlowModeDelay = limiterSlowModeDelay;
        this.convergenceFactor = convergenceFactor;
        this.failuresNoChangeToleranceRatio = failuresNoChangeToleranceRatio;
        this.failuresSpeedUpToleranceRatio = failuresSpeedUpToleranceRatio;
    }
}
