package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import java.time.Duration;

public interface KafkaReceiverParameters {

  Duration getPoolTimeout();

  int getReadQueueCapacity();

  boolean isWaitBetweenUnsuccessfulPolls();

  Duration getInitialIdleTime();

  Duration getMaxIdleTime();

  String getClientId();

  boolean isFilteringRateLimiterEnabled();

  boolean isFilteringEnabled();
}
