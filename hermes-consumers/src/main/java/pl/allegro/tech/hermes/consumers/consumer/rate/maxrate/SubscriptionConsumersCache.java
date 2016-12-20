package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignment;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentCaches;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentView;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SubscriptionConsumersCache {

    private final SubscriptionAssignmentCaches caches;

    @Inject
    public SubscriptionConsumersCache(SubscriptionAssignmentCaches caches) {
        this.caches = caches;
    }

    Map<SubscriptionName, Set<String>> getSubscriptionsConsumers() {
        List<SubscriptionAssignmentView> views = caches.all().stream()
                .map(SubscriptionAssignmentCache::createSnapshot)
                .collect(Collectors.toList());

        return views.stream()
                .flatMap(view -> view.getAllAssignments().stream())
                .collect(Collectors.groupingBy(
                        SubscriptionAssignment::getSubscriptionName,
                        Collectors.mapping(SubscriptionAssignment::getConsumerNodeId, Collectors.toSet())));
    }
}
