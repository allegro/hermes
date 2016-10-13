package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignment;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentPathSerializer;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentView;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentRegistry.AUTO_ASSIGNED_MARKER;

public class SubscriptionConsumersCache {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionConsumersCache.class);

    private final CuratorFramework curator;

    private final ZookeeperPaths zookeeperPaths;

    private final SubscriptionsCache subscriptionsCache;

    private final List<SubscriptionAssignmentCache> caches = new ArrayList<>();

    SubscriptionConsumersCache(CuratorFramework curator,
                                      ZookeeperPaths zookeeperPaths,
                                      SubscriptionsCache subscriptionsCache) {
        this.curator = curator;
        this.zookeeperPaths = zookeeperPaths;
        this.subscriptionsCache = subscriptionsCache;
    }

    void start() throws Exception {
        List<String> clusters = curator.getChildren().forPath(zookeeperPaths.consumersWorkloadPath());

        clusters.stream()
                .map(zookeeperPaths::consumersRuntimePath)
                .forEach(consumersRuntimePath ->
                        caches.add(new SubscriptionAssignmentCache(
                            curator,
                            consumersRuntimePath,
                            subscriptionsCache,
                            new SubscriptionAssignmentPathSerializer(consumersRuntimePath, AUTO_ASSIGNED_MARKER))
                        ));

        for (SubscriptionAssignmentCache cache : caches) {
            cache.start();
        }
    }

    void stop() {
        caches.forEach(cache -> {
            try {
                cache.stop();
            } catch (Exception e) {
                logger.warn("Unable to stop subscription assignment cache", e);
            }
        });
    }

    Map<SubscriptionName, Set<String>> getSubscriptionsConsumers() {
        List<SubscriptionAssignmentView> views = caches.stream()
                .map(SubscriptionAssignmentCache::createSnapshot)
                .collect(Collectors.toList());

        return views.stream()
                .flatMap(view -> view.getAllAssignments().stream())
                .collect(Collectors.groupingBy(
                        SubscriptionAssignment::getSubscriptionName,
                        Collectors.mapping(SubscriptionAssignment::getConsumerNodeId, Collectors.toSet())));
    }
}
