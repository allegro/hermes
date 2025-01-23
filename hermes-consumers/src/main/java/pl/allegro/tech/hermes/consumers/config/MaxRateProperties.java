package pl.allegro.tech.hermes.consumers.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateParameters;

@ConfigurationProperties(prefix = "consumer.maxrate")
public class MaxRateProperties implements MaxRateParameters {

  private RegistryBinaryEncoderProperties registryBinaryEncoder =
      new RegistryBinaryEncoderProperties();

  private Duration balanceInterval = Duration.ofSeconds(30);

  private Duration updateInterval = Duration.ofSeconds(15);

  private int historySize = 1;

  private double busyTolerance = 0.1;

  private double minMaxRate = 1.0;

  private double minAllowedChangePercent = 1.0;

  private double minSignificantUpdatePercent = 9.0;

  public RegistryBinaryEncoderProperties getRegistryBinaryEncoder() {
    return registryBinaryEncoder;
  }

  public void setRegistryBinaryEncoder(RegistryBinaryEncoderProperties registryBinaryEncoder) {
    this.registryBinaryEncoder = registryBinaryEncoder;
  }

  @Override
  public Duration getBalanceInterval() {
    return balanceInterval;
  }

  public void setBalanceInterval(Duration balanceInterval) {
    this.balanceInterval = balanceInterval;
  }

  @Override
  public Duration getUpdateInterval() {
    return updateInterval;
  }

  public void setUpdateInterval(Duration updateInterval) {
    this.updateInterval = updateInterval;
  }

  @Override
  public int getHistorySize() {
    return historySize;
  }

  public void setHistorySize(int historySize) {
    this.historySize = historySize;
  }

  @Override
  public double getBusyTolerance() {
    return busyTolerance;
  }

  public void setBusyTolerance(double busyTolerance) {
    this.busyTolerance = busyTolerance;
  }

  @Override
  public double getMinMaxRate() {
    return minMaxRate;
  }

  public void setMinMaxRate(double minMaxRate) {
    this.minMaxRate = minMaxRate;
  }

  @Override
  public double getMinAllowedChangePercent() {
    return minAllowedChangePercent;
  }

  public void setMinAllowedChangePercent(double minAllowedChangePercent) {
    this.minAllowedChangePercent = minAllowedChangePercent;
  }

  @Override
  public double getMinSignificantUpdatePercent() {
    return minSignificantUpdatePercent;
  }

  public void setMinSignificantUpdatePercent(double minSignificantUpdatePercent) {
    this.minSignificantUpdatePercent = minSignificantUpdatePercent;
  }
}
