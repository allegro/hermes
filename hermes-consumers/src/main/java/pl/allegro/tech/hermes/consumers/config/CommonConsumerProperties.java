package pl.allegro.tech.hermes.consumers.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.consumers.CommonConsumerParameters;
import pl.allegro.tech.hermes.consumers.consumer.SerialConsumerParameters;
import pl.allegro.tech.hermes.consumers.supervisor.SupervisorParameters;

@ConfigurationProperties(prefix = "consumer")
public class CommonConsumerProperties implements CommonConsumerParameters {

  private int threadPoolSize = 500;

  private int healthCheckPort = 8000;

  private Duration subscriptionIdsCacheRemovedExpireAfterAccess = Duration.ofSeconds(60);

  private SupervisorParameters backgroundSupervisor = new BackgroundSupervisor();

  private SerialConsumerParameters serialConsumer = new SerialConsumer();

  private int signalProcessingQueueSize = 5_000;

  private boolean useTopicMessageSizeEnabled = false;

  private Duration undeliveredMessageLogPersistPeriod = Duration.ofSeconds(5);

  public int getThreadPoolSize() {
    return threadPoolSize;
  }

  public void setThreadPoolSize(int threadPoolSize) {
    this.threadPoolSize = threadPoolSize;
  }

  public int getHealthCheckPort() {
    return healthCheckPort;
  }

  public void setHealthCheckPort(int healthCheckPort) {
    this.healthCheckPort = healthCheckPort;
  }

  public Duration getSubscriptionIdsCacheRemovedExpireAfterAccess() {
    return subscriptionIdsCacheRemovedExpireAfterAccess;
  }

  public void setSubscriptionIdsCacheRemovedExpireAfterAccess(
      Duration subscriptionIdsCacheRemovedExpireAfterAccess) {
    this.subscriptionIdsCacheRemovedExpireAfterAccess =
        subscriptionIdsCacheRemovedExpireAfterAccess;
  }

  @Override
  public SupervisorParameters getBackgroundSupervisor() {
    return backgroundSupervisor;
  }

  public void setBackgroundSupervisor(SupervisorParameters backgroundSupervisor) {
    this.backgroundSupervisor = backgroundSupervisor;
  }

  @Override
  public SerialConsumerParameters getSerialConsumer() {
    return serialConsumer;
  }

  public void setSerialConsumer(SerialConsumerParameters serialConsumer) {
    this.serialConsumer = serialConsumer;
  }

  @Override
  public int getSignalProcessingQueueSize() {
    return signalProcessingQueueSize;
  }

  public void setSignalProcessingQueueSize(int signalProcessingQueueSize) {
    this.signalProcessingQueueSize = signalProcessingQueueSize;
  }

  @Override
  public boolean isUseTopicMessageSizeEnabled() {
    return useTopicMessageSizeEnabled;
  }

  public void setUseTopicMessageSizeEnabled(boolean useTopicMessageSizeEnabled) {
    this.useTopicMessageSizeEnabled = useTopicMessageSizeEnabled;
  }

  public Duration getUndeliveredMessageLogPersistPeriod() {
    return undeliveredMessageLogPersistPeriod;
  }

  public void setUndeliveredMessageLogPersistPeriod(Duration undeliveredMessageLogPersistPeriod) {
    this.undeliveredMessageLogPersistPeriod = undeliveredMessageLogPersistPeriod;
  }

  public static final class SerialConsumer implements SerialConsumerParameters {

    private Duration signalProcessingInterval = Duration.ofMillis(5_000);

    private int inflightSize = 100;

    @Override
    public Duration getSignalProcessingInterval() {
      return signalProcessingInterval;
    }

    public void setSignalProcessingInterval(Duration signalProcessingInterval) {
      this.signalProcessingInterval = signalProcessingInterval;
    }

    @Override
    public int getInflightSize() {
      return inflightSize;
    }

    public void setInflightSize(int inflightSize) {
      this.inflightSize = inflightSize;
    }
  }

  public static final class BackgroundSupervisor implements SupervisorParameters {

    private Duration interval = Duration.ofMillis(20_000);

    private Duration unhealthyAfter = Duration.ofMillis(600_000);

    private Duration killAfter = Duration.ofMillis(300_000);

    @Override
    public Duration getInterval() {
      return interval;
    }

    public void setInterval(Duration interval) {
      this.interval = interval;
    }

    @Override
    public Duration getUnhealthyAfter() {
      return unhealthyAfter;
    }

    public void setUnhealthyAfter(Duration unhealthyAfter) {
      this.unhealthyAfter = unhealthyAfter;
    }

    @Override
    public Duration getKillAfter() {
      return killAfter;
    }

    public void setKillAfter(Duration killAfter) {
      this.killAfter = killAfter;
    }
  }
}
