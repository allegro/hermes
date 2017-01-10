package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.HierarchicalCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SubscriptionAssignmentRegistry {

    private static final int SUBSCRIPTION_LEVEL = 0;

    private static final int ASSIGNMENT_LEVEL = 1;

    private final Set<SubscriptionAssignment> assignments = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Set<SubscriptionAssignmentAware> callbacks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final String consumerNodeId;

    private final CuratorFramework curator;

    private final HierarchicalCache cache;

    private final SubscriptionsCache subscriptionsCache;

    private final SubscriptionAssignmentPathSerializer pathSerializer;

    public SubscriptionAssignmentRegistry(String consumerNodeId,
                                          CuratorFramework curator,
                                          String basePath,
                                          SubscriptionsCache subscriptionsCache,
                                          SubscriptionAssignmentPathSerializer pathSerializer) {
        this.consumerNodeId = consumerNodeId;
        this.curator = curator;
        this.subscriptionsCache = subscriptionsCache;
        this.pathSerializer = pathSerializer;
        this.cache = new HierarchicalCache(
                curator, Executors.newSingleThreadScheduledExecutor(), basePath, 2, Collections.emptyList()
        );

        cache.registerCallback(ASSIGNMENT_LEVEL, (e) -> {
            SubscriptionAssignment assignment = pathSerializer.deserialize(e.getData().getPath());
            switch (e.getType()) {
                case CHILD_ADDED:
                    onAssignmentAdded(assignment);
                    break;
                case CHILD_REMOVED:
                    onAssignmentRemoved(assignment);
                    break;
            }
        });
    }

    public void start() throws Exception {
        readExistingAssignments().forEach(this::onAssignmentAdded);
        cache.start();
    }

    public void stop() throws Exception {
        cache.stop();
    }

    public void registerAssignmentCallback(SubscriptionAssignmentAware callback) {
        callbacks.add(callback);
    }

    public boolean isAssignedTo(String nodeId, SubscriptionName subscription) {
        return assignments.stream().anyMatch(a -> a.getSubscriptionName().equals(subscription) && a.getConsumerNodeId().equals(nodeId));
    }

    public SubscriptionAssignmentView createSnapshot() {
        Map<SubscriptionName, Set<SubscriptionAssignment>> snapshot = new HashMap<>();
        for (SubscriptionAssignment assignment : assignments) {
            snapshot.compute(assignment.getSubscriptionName(), (k, v) -> {
                v = (v == null ? new HashSet<>() : v);
                v.add(assignment);
                return v;
            });
        }
        return new SubscriptionAssignmentView(snapshot);
    }

    private List<SubscriptionAssignment> readExistingAssignments() {
        return subscriptionsCache.listActiveSubscriptionNames().stream().flatMap(subscriptionName -> {
            String path = pathSerializer.serialize(subscriptionName);
            List<String> children = new ArrayList<>();
            try {
                children.addAll(curator.getChildren().forPath(path));
            } catch (Exception e) {
                // ignore - should be fixed by the cache
            }
            return children.stream().map(node -> new SubscriptionAssignment(node, subscriptionName));
        }).collect(Collectors.toList());
    }

    private void onAssignmentAdded(SubscriptionAssignment assignment) {
        assignments.add(assignment);
        if (consumerNodeId.equals(assignment.getConsumerNodeId())) {
            callbacks.forEach(callback ->
                callback.onSubscriptionAssigned(assignment.getSubscriptionName())
            );
        }
    }

    private void onAssignmentRemoved(SubscriptionAssignment assignment) {
        assignments.remove(assignment);
        if (consumerNodeId.equals(assignment.getConsumerNodeId())) {
            callbacks.forEach(callback ->
                    callback.onAssignmentRemoved(assignment.getSubscriptionName())
            );
        }
        removeSubscriptionEntryIfEmpty(assignment.getSubscriptionName());
    }

    private void removeSubscriptionEntryIfEmpty(SubscriptionName subscriptionName) {
        askCuratorPolitely(() -> {
            if (curator.getChildren().forPath(pathSerializer.serialize(subscriptionName)).isEmpty()) {
                curator.delete().guaranteed().forPath(pathSerializer.serialize(subscriptionName));
            }
        });
    }

    public void dropAssignment(SubscriptionAssignment assignment) {
        askCuratorPolitely(() -> curator.delete().guaranteed()
                .forPath(pathSerializer.serialize(assignment.getSubscriptionName(), assignment.getConsumerNodeId())));
    }

    public void addPersistentAssignment(SubscriptionAssignment assignment) {
        addAssignment(assignment, CreateMode.PERSISTENT);
    }

    public void addEphemeralAssignment(SubscriptionAssignment assignment) {
        addAssignment(assignment, CreateMode.EPHEMERAL);
    }

    private void addAssignment(SubscriptionAssignment assignment, CreateMode createMode) {
        askCuratorPolitely(() -> curator.create().creatingParentsIfNeeded().withMode(createMode)
                .forPath(pathSerializer.serialize(assignment.getSubscriptionName(), assignment.getConsumerNodeId())));
    }

    interface CuratorTask {
        void run() throws Exception;
    }

    private void askCuratorPolitely(CuratorTask task) {
        try {
            task.run();
        } catch (KeeperException.NodeExistsException | KeeperException.NoNodeException ex) {
            // ignore
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }
}
