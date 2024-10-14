package pl.allegro.tech.hermes.consumers.supervisor.workload;

import java.time.Duration;

public interface WorkBalancingParameters {

  Duration getRebalanceInterval();

  int getConsumersPerSubscription();

  int getMaxSubscriptionsPerConsumer();

  boolean isAutoRebalance();
}
