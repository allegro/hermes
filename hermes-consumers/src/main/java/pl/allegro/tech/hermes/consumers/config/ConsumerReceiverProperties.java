package pl.allegro.tech.hermes.consumers.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.util.InetAddressInstanceIdResolver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaReceiverParameters;

@ConfigurationProperties(prefix = "consumer.receiver")
public class ConsumerReceiverProperties implements KafkaReceiverParameters {

  private Duration poolTimeout = Duration.ofMillis(30);

  private int readQueueCapacity = 1000;

  private boolean waitBetweenUnsuccessfulPolls = true;

  private Duration initialIdleTime = Duration.ofMillis(10);

  private Duration maxIdleTime = Duration.ofMillis(1000);

  private String clientId = new InetAddressInstanceIdResolver().resolve();

  private boolean filteringRateLimiterEnabled = true;

  private boolean filteringEnabled = true;

  @Override
  public Duration getPoolTimeout() {
    return poolTimeout;
  }

  public void setPoolTimeout(Duration poolTimeout) {
    this.poolTimeout = poolTimeout;
  }

  @Override
  public int getReadQueueCapacity() {
    return readQueueCapacity;
  }

  public void setReadQueueCapacity(int readQueueCapacity) {
    this.readQueueCapacity = readQueueCapacity;
  }

  @Override
  public boolean isWaitBetweenUnsuccessfulPolls() {
    return waitBetweenUnsuccessfulPolls;
  }

  public void setWaitBetweenUnsuccessfulPolls(boolean waitBetweenUnsuccessfulPolls) {
    this.waitBetweenUnsuccessfulPolls = waitBetweenUnsuccessfulPolls;
  }

  @Override
  public Duration getInitialIdleTime() {
    return initialIdleTime;
  }

  public void setInitialIdleTime(Duration initialIdleTime) {
    this.initialIdleTime = initialIdleTime;
  }

  @Override
  public Duration getMaxIdleTime() {
    return maxIdleTime;
  }

  public void setMaxIdleTime(Duration maxIdleTime) {
    this.maxIdleTime = maxIdleTime;
  }

  @Override
  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  @Override
  public boolean isFilteringRateLimiterEnabled() {
    return filteringRateLimiterEnabled;
  }

  public void setFilteringRateLimiterEnabled(boolean filteringRateLimiterEnabled) {
    this.filteringRateLimiterEnabled = filteringRateLimiterEnabled;
  }

  @Override
  public boolean isFilteringEnabled() {
    return filteringEnabled;
  }

  public void setFilteringEnabled(boolean filteringEnabled) {
    this.filteringEnabled = filteringEnabled;
  }
}
