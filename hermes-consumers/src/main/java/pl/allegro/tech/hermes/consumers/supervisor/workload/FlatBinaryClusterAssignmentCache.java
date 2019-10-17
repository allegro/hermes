package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

public class FlatBinaryClusterAssignmentCache implements ClusterAssignmentCache {

    private static final Logger logger = getLogger(FlatBinaryClusterAssignmentCache.class);

    private final ConsumerNodesRegistry consumerNodesRegistry;
    private final FlatBinaryWorkloadRegistryPaths paths;
    private final ConsumerWorkloadDecoder consumerWorkloadDecoder;
    private final String consumersPath;
    private final ZookeeperOperations zookeeper;

    private final Map<String, Set<SubscriptionName>> consumerSubscriptions = new ConcurrentHashMap<>();
    private final Set<SubscriptionAssignment> assignments = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public FlatBinaryClusterAssignmentCache(CuratorFramework curator,
                                            String clusterName,
                                            ZookeeperPaths zookeeperPaths,
                                            SubscriptionIds subscriptionIds,
                                            ConsumerNodesRegistry consumerNodesRegistry) {
        this.zookeeper = new ZookeeperOperations(curator);
        this.consumerNodesRegistry = consumerNodesRegistry;
        this.paths = new FlatBinaryWorkloadRegistryPaths(zookeeperPaths, clusterName);
        this.consumerWorkloadDecoder = new ConsumerWorkloadDecoder(subscriptionIds);
        this.consumersPath = paths.consumersWorkloadCurrentClusterRuntimeBinaryPath();
    }

    @Override
    public boolean isReady() {
        return true; // manually refreshed, always ready
    }

    @Override
    public void refresh() {
        logger.info("Refreshing consumer workload assignments");
        List<String> currentlyAssignedConsumers = getWorkloadConsumers();
        List<String> activeConsumers = consumerNodesRegistry.listConsumerNodes();

        assignments.clear();
        consumerSubscriptions.clear();

        for (String consumer : currentlyAssignedConsumers) {
            if (activeConsumers.contains(consumer)) {
                Set<SubscriptionName> subscriptions = readConsumerWorkload(consumer);
                consumerSubscriptions.put(consumer, subscriptions);
                subscriptions.forEach(subscription -> {
                    assignments.add(new SubscriptionAssignment(consumer, subscription));
                });
            } else {
                logger.info("Deleting consumer {} from workload", consumer);
                deleteConsumerWorkloadNode(consumer);
            }
        }
    }

    private Set<SubscriptionName> readConsumerWorkload(String consumer) {
        Optional<byte[]> nodeData = zookeeper.getNodeData(paths.consumerWorkloadPath(consumer));
        if (nodeData.isPresent()) {
            byte[] data = nodeData.get();
            Set<SubscriptionName> subscriptions = consumerWorkloadDecoder.decode(data);
            logger.info("Decoded {} bytes of assignments for consumer {} with {} subscription entries",
                    data.length, consumer, subscriptions.size());
            return subscriptions;
        } else {
            logger.info("No workload data available for consumer {}", consumer);
            return Collections.emptySet();
        }
    }

    private void deleteConsumerWorkloadNode(String consumer) {
        String path = paths.consumerWorkloadPath(consumer);
        try {
            zookeeper.deleteNode(path);
        } catch (Exception e) {
            logger.warn("Could not delete consumer workload node at {}", path, e);
        }
    }

    private List<String> getWorkloadConsumers() {
        try {
            if (zookeeper.exists(consumersPath)) {
                return zookeeper.getNodeChildren(consumersPath);
            }
        } catch (Exception e) {
            logger.warn("Could not get workload consumer nodes list", e);
        }
        return Collections.emptyList();
    }

    @Override
    public SubscriptionAssignmentView createSnapshot() {
        return SubscriptionAssignmentView.of(assignments);
    }

    @Override
    public boolean isAssignedTo(String consumerId, SubscriptionName subscription) {
        Set<SubscriptionName> subscriptions = consumerSubscriptions.getOrDefault(consumerId, Collections.emptySet());
        return subscriptions.contains(subscription);
    }

    @Override
    public Map<SubscriptionName, Set<String>> getSubscriptionConsumers() {
        Map<SubscriptionName, Set<String>> result = new HashMap<>();
        consumerSubscriptions.forEach((consumer, subscriptions) -> {
            subscriptions.forEach(subscription -> {
                if (result.containsKey(subscription)) {
                    result.get(subscription).add(consumer);
                } else {
                    HashSet<String> consumers = new HashSet<>();
                    consumers.add(consumer);
                    result.put(subscription, consumers);
                }
            });
        });
        return result;
    }

    @Override
    public Set<String> getAssignedConsumers() {
        return consumerSubscriptions.keySet();
    }

    @Override
    public Set<SubscriptionName> getConsumerSubscriptions(String consumerId) {
        return consumerSubscriptions.getOrDefault(consumerId, Collections.emptySet());
    }
}
