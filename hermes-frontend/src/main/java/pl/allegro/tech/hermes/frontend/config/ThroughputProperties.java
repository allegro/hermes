package pl.allegro.tech.hermes.frontend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputParameters;

@ConfigurationProperties(prefix = "frontend.throughput")
public class ThroughputProperties implements ThroughputParameters {

  private String type = "unlimited";

  private long fixedMax = Long.MAX_VALUE;

  private long dynamicMax = Long.MAX_VALUE;

  private long dynamicThreshold = Long.MAX_VALUE;

  private long dynamicDesired = Long.MAX_VALUE;

  private double dynamicIdle = 0.5;

  private Duration dynamicCheckInterval = Duration.ofSeconds(30);

  @Override
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public long getFixedMax() {
    return fixedMax;
  }

  public void setFixedMax(long fixedMax) {
    this.fixedMax = fixedMax;
  }

  @Override
  public long getDynamicMax() {
    return dynamicMax;
  }

  public void setDynamicMax(long dynamicMax) {
    this.dynamicMax = dynamicMax;
  }

  @Override
  public long getDynamicThreshold() {
    return dynamicThreshold;
  }

  public void setDynamicThreshold(long dynamicThreshold) {
    this.dynamicThreshold = dynamicThreshold;
  }

  @Override
  public long getDynamicDesired() {
    return dynamicDesired;
  }

  public void setDynamicDesired(long dynamicDesired) {
    this.dynamicDesired = dynamicDesired;
  }

  @Override
  public double getDynamicIdle() {
    return dynamicIdle;
  }

  public void setDynamicIdle(double dynamicIdle) {
    this.dynamicIdle = dynamicIdle;
  }

  @Override
  public Duration getDynamicCheckInterval() {
    return dynamicCheckInterval;
  }

  public void setDynamicCheckInterval(Duration dynamicCheckInterval) {
    this.dynamicCheckInterval = dynamicCheckInterval;
  }
}
