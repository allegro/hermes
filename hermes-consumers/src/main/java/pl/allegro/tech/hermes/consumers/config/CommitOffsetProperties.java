package pl.allegro.tech.hermes.consumers.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer.commit.offset")
public class CommitOffsetProperties {

  private Duration period = Duration.ofSeconds(60);

  private int queuesSize = 200_000;

  public Duration getPeriod() {
    return period;
  }

  public void setPeriod(Duration period) {
    this.period = period;
  }

  public int getQueuesSize() {
    return queuesSize;
  }

  public void setQueuesSize(int queuesSize) {
    this.queuesSize = queuesSize;
  }
}
