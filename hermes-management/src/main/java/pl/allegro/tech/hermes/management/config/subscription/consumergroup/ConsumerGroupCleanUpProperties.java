package pl.allegro.tech.hermes.management.config.subscription.consumergroup;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupCleanUpParameters;

@ConfigurationProperties(prefix = "management.consumer-group.clean-up")
public class ConsumerGroupCleanUpProperties implements ConsumerGroupCleanUpParameters {
  private boolean enabled = true;
  private Duration interval = Duration.ofMinutes(5);
  private Duration initialDelay = Duration.ofMinutes(1);
  private Duration timeout = Duration.ofHours(24);
  private boolean removeTasksAfterTimeout = true;

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public Duration getInterval() {
    return interval;
  }

  public void setInterval(Duration interval) {
    this.interval = interval;
  }

  @Override
  public Duration getInitialDelay() {
    return initialDelay;
  }

  public void setInitialDelay(Duration initialDelay) {
    this.initialDelay = initialDelay;
  }

  @Override
  public Duration getTimeout() {
    return timeout;
  }

  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }

  public void setRemoveTasksAfterTimeout(boolean removeTasksAfterTimeout) {
    this.removeTasksAfterTimeout = removeTasksAfterTimeout;
  }

  @Override
  public boolean isRemoveTasksAfterTimeout() {
    return removeTasksAfterTimeout;
  }
}
