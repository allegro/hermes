package pl.allegro.tech.hermes.consumers.supervisor.workload.selective;

import java.time.Duration;

public interface SelectiveSupervisorParameters {

    Duration getRebalanceInterval();

    int getConsumersPerSubscription();

    int getMaxSubscriptionsPerConsumer();

    String getNodeId();

    boolean isAutoRebalance();
}
