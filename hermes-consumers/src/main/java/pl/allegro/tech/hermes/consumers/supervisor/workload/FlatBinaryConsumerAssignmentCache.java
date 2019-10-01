package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

public class FlatBinaryConsumerAssignmentCache implements ConsumerAssignmentCache, NodeCacheListener {

    private static final Logger logger = getLogger(FlatBinaryConsumerAssignmentCache.class);

    private final FlatBinaryWorkloadRegistryPaths paths;
    private final String consumerId;
    private final NodeCache workloadNodeCache;
    private final ConsumerWorkloadDecoder consumerWorkloadDecoder;
    private final Set<SubscriptionName> currentlyAssignedSubscriptions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<SubscriptionAssignmentAware> callbacks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private volatile boolean started = false;

    public FlatBinaryConsumerAssignmentCache(
            CuratorFramework curator,
            String consumerId,
            String clusterName,
            ZookeeperPaths zookeeperPaths,
            SubscriptionIds subscriptionIds
    ) {
        this.paths = new FlatBinaryWorkloadRegistryPaths(zookeeperPaths, clusterName);
        this.consumerId = consumerId;

        String path = paths.consumerWorkloadPath(consumerId);
        this.workloadNodeCache = new NodeCache(curator, path);
        workloadNodeCache.getListenable().addListener(this);

        this.consumerWorkloadDecoder = new ConsumerWorkloadDecoder(subscriptionIds);
    }

    @Override
    public void start() throws Exception {
        try {
            logger.info("Starting flat binary workload assignment cache at {}, watching current consumer path at {}",
                    paths.consumersWorkloadCurrentClusterRuntimeBinaryPath(), paths.consumerWorkloadPath(consumerId));
            workloadNodeCache.start(true);
        } catch (Exception e) {
            throw new IllegalStateException("Could not start node cache for consumer workload", e);
        }
        refreshConsumerWorkload();
        started = true;
    }

    private void refreshConsumerWorkload() {
        ChildData nodeData = workloadNodeCache.getCurrentData();
        if (nodeData != null) {
            byte[] data = nodeData.getData();
            Set<SubscriptionName> subscriptions = consumerWorkloadDecoder.decode(data);
            logger.info("Decoded {} bytes of assignments for current node with {} subscription entries", data.length, subscriptions.size());

            updateAssignedSubscriptions(subscriptions);
        } else {
            logger.info("No workload data available for consumer");
        }
    }

    private void updateAssignedSubscriptions(Set<SubscriptionName> targetAssignments) {
        ImmutableSet<SubscriptionName> assignmentDeletions = Sets.difference(currentlyAssignedSubscriptions, targetAssignments)
                .immutableCopy();
        ImmutableSet<SubscriptionName> assignmentsAdditions = Sets.difference(targetAssignments, currentlyAssignedSubscriptions)
                .immutableCopy();


        assignmentDeletions.forEach(s -> logger.info("Assignment deletion for subscription {}", s.getQualifiedName()));
        assignmentsAdditions.forEach(s -> logger.info("Assignment addition for subscription {}", s.getQualifiedName()));

        currentlyAssignedSubscriptions.clear();
        currentlyAssignedSubscriptions.addAll(targetAssignments);

        callbacks.forEach(callback -> {
            if (!callback.watchedConsumerId().isPresent() || callback.watchedConsumerId().get().equals(consumerId)) {
                assignmentDeletions.forEach(callback::onAssignmentRemoved);
                assignmentsAdditions.forEach(callback::onSubscriptionAssigned);
            }
        });
    }

    @Override
    public void stop() throws Exception {
        try {
            logger.info("Stopping flat binary workload assignment cache");
            started = false;
            workloadNodeCache.close();
        } catch (IOException e) {
            throw new RuntimeException("Could not stop node cache for consumer workload", e);
        }
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isAssignedTo(SubscriptionName subscription) {
        return currentlyAssignedSubscriptions.contains(subscription);
    }

    @Override
    public void registerAssignmentCallback(SubscriptionAssignmentAware callback) {
        callbacks.add(callback);
    }

    @Override
    public Set<SubscriptionName> getConsumerSubscriptions() {
        return ImmutableSet.copyOf(currentlyAssignedSubscriptions);
    }

    @Override
    public void nodeChanged() throws Exception {
        refreshConsumerWorkload();
    }
}
