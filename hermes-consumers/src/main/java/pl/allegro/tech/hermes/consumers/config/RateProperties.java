package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.RateCalculatorParameters;

import java.time.Duration;

@ConfigurationProperties(prefix = "consumer.rate")
public class RateProperties {

    private Duration limiterSupervisorPeriod = Duration.ofSeconds(30);

    private int limiterReportingThreadPoolSize = 30;

    private boolean limiterReportingThreadMonitoringEnabled = false;

    private Duration limiterHeartbeatModeDelay = Duration.ofSeconds(60);

    private Duration limiterSlowModeDelay = Duration.ofSeconds(60);

    private double convergenceFactor = 0.2;

    private double failuresNoChangeToleranceRatio = 0.05;

    private double failuresSpeedUpToleranceRatio = 0.01;

    public Duration getLimiterSupervisorPeriod() {
        return limiterSupervisorPeriod;
    }

    public void setLimiterSupervisorPeriod(Duration limiterSupervisorPeriod) {
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

    public Duration getLimiterHeartbeatModeDelay() {
        return limiterHeartbeatModeDelay;
    }

    public void setLimiterHeartbeatModeDelay(Duration limiterHeartbeatModeDelay) {
        this.limiterHeartbeatModeDelay = limiterHeartbeatModeDelay;
    }

    public Duration getLimiterSlowModeDelay() {
        return limiterSlowModeDelay;
    }

    public void setLimiterSlowModeDelay(Duration limiterSlowModeDelay) {
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

    protected RateCalculatorParameters toRateCalculatorParameters() {
        return new RateCalculatorParameters(
                this.limiterHeartbeatModeDelay,
                this.limiterSlowModeDelay,
                this.convergenceFactor,
                this.failuresNoChangeToleranceRatio,
                this.failuresSpeedUpToleranceRatio
        );
    }
}
