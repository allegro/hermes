package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

public class RateCalculatorParameters {

    private final int limiterHeartbeatModeDelay;

    private final int limiterSlowModeDelay;

    private final double convergenceFactor;

    private final double failuresNoChangeToleranceRatio;

    private final double failuresSpeedUpToleranceRatio;

    public int getLimiterHeartbeatModeDelay() {
        return limiterHeartbeatModeDelay;
    }

    public int getLimiterSlowModeDelay() {
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

    public RateCalculatorParameters(int limiterHeartbeatModeDelay,
                                    int limiterSlowModeDelay,
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
