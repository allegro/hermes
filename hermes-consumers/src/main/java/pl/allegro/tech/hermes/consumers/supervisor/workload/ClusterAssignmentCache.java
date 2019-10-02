package pl.allegro.tech.hermes.consumers.supervisor.workload;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Map;
import java.util.Set;

public interface ClusterAssignmentCache {

    boolean isReady();

    void refresh();

    SubscriptionAssignmentView createSnapshot();

    boolean isAssignedTo(String nodeId, SubscriptionName subscription);

    Map<SubscriptionName, Set<String>> getSubscriptionConsumers();

    Set<String> getAssignedConsumers();

    Set<SubscriptionName> getConsumerSubscriptions(String nodeId);
}
