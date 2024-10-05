package pl.allegro.tech.hermes.frontend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend.message.preview")
public class MessagePreviewProperties {

  private boolean enabled = false;

  private int maxSizeKb = 10;

  private int size = 3;

  private Duration logPersistPeriod = Duration.ofSeconds(30);

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getMaxSizeKb() {
    return maxSizeKb;
  }

  public void setMaxSizeKb(int maxSizeKb) {
    this.maxSizeKb = maxSizeKb;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public Duration getLogPersistPeriod() {
    return logPersistPeriod;
  }

  public void setLogPersistPeriod(Duration logPersistPeriod) {
    this.logPersistPeriod = logPersistPeriod;
  }
}
