package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.RateCalculatorParameters;

@ConfigurationProperties(prefix = "consumer.rate")
public class RateProperties {

    private int limiterSupervisorPeriod = 30;

    private int limiterReportingThreadPoolSize = 30;

    private boolean limiterReportingThreadMonitoringEnabled = false;

    private int limiterHeartbeatModeDelay = 60;

    private int limiterSlowModeDelay = 60;

    private double convergenceFactor = 0.2;

    private double failuresNoChangeToleranceRatio = 0.05;

    private double failuresSpeedUpToleranceRatio = 0.01;

    public int getLimiterSupervisorPeriod() {
        return limiterSupervisorPeriod;
    }

    public void setLimiterSupervisorPeriod(int limiterSupervisorPeriod) {
        this.limiterSupervisorPeriod = limiterSupervisorPeriod;
    }

    public int getLimiterReportingThreadPoolSize() {
        return limiterReportingThreadPoolSize;
    }

    public void setLimiterReportingThreadPoolSize(int limiterReportingThreadPoolSize) {
        this.limiterReportingThreadPoolSize = limiterReportingThreadPoolSize;
    }

    public boolean isLimiterReportingThreadMonitoringEnabled() {
        return limiterReportingThreadMonitoringEnabled;
    }

    public void setLimiterReportingThreadMonitoringEnabled(boolean limiterReportingThreadMonitoringEnabled) {
        this.limiterReportingThreadMonitoringEnabled = limiterReportingThreadMonitoringEnabled;
    }

    public int getLimiterHeartbeatModeDelay() {
        return limiterHeartbeatModeDelay;
    }

    public void setLimiterHeartbeatModeDelay(int limiterHeartbeatModeDelay) {
        this.limiterHeartbeatModeDelay = limiterHeartbeatModeDelay;
    }

    public int getLimiterSlowModeDelay() {
        return limiterSlowModeDelay;
    }

    public void setLimiterSlowModeDelay(int limiterSlowModeDelay) {
        this.limiterSlowModeDelay = limiterSlowModeDelay;
    }

    public double getConvergenceFactor() {
        return convergenceFactor;
    }

    public void setConvergenceFactor(double convergenceFactor) {
        this.convergenceFactor = convergenceFactor;
    }

    public double getFailuresNoChangeToleranceRatio() {
        return failuresNoChangeToleranceRatio;
    }

    public void setFailuresNoChangeToleranceRatio(double failuresNoChangeToleranceRatio) {
        this.failuresNoChangeToleranceRatio = failuresNoChangeToleranceRatio;
    }

    public double getFailuresSpeedUpToleranceRatio() {
        return failuresSpeedUpToleranceRatio;
    }

    public void setFailuresSpeedUpToleranceRatio(double failuresSpeedUpToleranceRatio) {
        this.failuresSpeedUpToleranceRatio = failuresSpeedUpToleranceRatio;
    }

    public RateCalculatorParameters toRateCalculatorParameters() {
        return new RateCalculatorParameters(
                this.limiterHeartbeatModeDelay,
                this.limiterSlowModeDelay,
                this.convergenceFactor,
                this.failuresNoChangeToleranceRatio,
                this.failuresSpeedUpToleranceRatio
        );
    }
}
