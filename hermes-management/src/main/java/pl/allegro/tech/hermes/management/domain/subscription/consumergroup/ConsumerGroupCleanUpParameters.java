package pl.allegro.tech.hermes.management.domain.subscription.consumergroup;

import java.time.Duration;

public interface ConsumerGroupCleanUpParameters {

  boolean isEnabled();

  Duration getInterval();

  Duration getInitialDelay();

  Duration getTimeout();

  boolean isRemoveTasksAfterTimeout();
}
