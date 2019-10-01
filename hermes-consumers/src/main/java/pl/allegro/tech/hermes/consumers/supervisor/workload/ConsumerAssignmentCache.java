package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Set;

public interface ConsumerAssignmentCache {

    void start() throws Exception;

    void stop() throws Exception;

    boolean isStarted();

    boolean isAssignedTo(SubscriptionName subscription);

    void registerAssignmentCallback(SubscriptionAssignmentAware callback);

    Set<SubscriptionName> getConsumerSubscriptions();
}
