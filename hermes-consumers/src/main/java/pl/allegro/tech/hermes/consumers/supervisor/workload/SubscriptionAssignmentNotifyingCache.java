package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Map;
import java.util.Set;

public interface SubscriptionAssignmentNotifyingCache {

    void start() throws Exception;

    void stop() throws Exception;

    boolean isStarted();

    SubscriptionAssignmentView createSnapshot();

    boolean isAssignedTo(String nodeId, SubscriptionName subscription);

    void registerAssignmentCallback(SubscriptionAssignmentAware callback);

    Map<SubscriptionName, Set<String>> getSubscriptionConsumers();

    Set<SubscriptionName> getConsumerSubscriptions(String consumerId);

    Set<String> getAssignedConsumers();
}
