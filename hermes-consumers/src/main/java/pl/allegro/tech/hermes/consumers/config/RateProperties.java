package pl.allegro.tech.hermes.consumers.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.RateCalculatorParameters;

@ConfigurationProperties(prefix = "consumer.rate")
public class RateProperties implements RateCalculatorParameters {

  private Duration limiterSupervisorPeriod = Duration.ofSeconds(30);

  private int limiterReportingThreadPoolSize = 30;

  private boolean limiterReportingThreadMonitoringEnabled = false;

  private Duration limiterHeartbeatModeDelay = Duration.ofMinutes(1);

  private Duration limiterSlowModeDelay = Duration.ofSeconds(1);

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

  public void setLimiterReportingThreadMonitoringEnabled(
      boolean limiterReportingThreadMonitoringEnabled) {
    this.limiterReportingThreadMonitoringEnabled = limiterReportingThreadMonitoringEnabled;
  }

  @Override
  public Duration getLimiterHeartbeatModeDelay() {
    return limiterHeartbeatModeDelay;
  }

  public void setLimiterHeartbeatModeDelay(Duration limiterHeartbeatModeDelay) {
    this.limiterHeartbeatModeDelay = limiterHeartbeatModeDelay;
  }

  @Override
  public Duration getLimiterSlowModeDelay() {
    return limiterSlowModeDelay;
  }

  public void setLimiterSlowModeDelay(Duration limiterSlowModeDelay) {
    this.limiterSlowModeDelay = limiterSlowModeDelay;
  }

  @Override
  public double getConvergenceFactor() {
    return convergenceFactor;
  }

  public void setConvergenceFactor(double convergenceFactor) {
    this.convergenceFactor = convergenceFactor;
  }

  @Override
  public double getFailuresNoChangeToleranceRatio() {
    return failuresNoChangeToleranceRatio;
  }

  public void setFailuresNoChangeToleranceRatio(double failuresNoChangeToleranceRatio) {
    this.failuresNoChangeToleranceRatio = failuresNoChangeToleranceRatio;
  }

  @Override
  public double getFailuresSpeedUpToleranceRatio() {
    return failuresSpeedUpToleranceRatio;
  }

  public void setFailuresSpeedUpToleranceRatio(double failuresSpeedUpToleranceRatio) {
    this.failuresSpeedUpToleranceRatio = failuresSpeedUpToleranceRatio;
  }
}
