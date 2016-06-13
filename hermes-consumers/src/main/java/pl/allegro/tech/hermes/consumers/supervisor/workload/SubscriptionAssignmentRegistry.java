package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.HierarchicalCache;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class SubscriptionAssignmentRegistry {

    private static final int SUBSCRIPTION_LEVEL = 0;

    private static final int ASSIGNMENT_LEVEL = 1;

    private final Set<SubscriptionAssignment> assignments = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final CuratorFramework curator;

    private final HierarchicalCache cache;

    private final SubscriptionsCache subscriptionsCache;

    private final SubscriptionAssignmentPathSerializer pathSerializer;

    public SubscriptionAssignmentRegistry(CuratorFramework curator, String path,
                                          SubscriptionsCache subscriptionsCache, SubscriptionAssignmentPathSerializer pathSerializer) {
        this.curator = curator;
        this.subscriptionsCache = subscriptionsCache;
        this.pathSerializer = pathSerializer;
        this.cache = new HierarchicalCache(
                curator, Executors.newSingleThreadScheduledExecutor(), path, 2, Collections.emptyList()
        );

        cache.registerCallback(ASSIGNMENT_LEVEL, (e) -> {
            SubscriptionAssignment assignment = pathSerializer.deserialize(e.getData().getPath());
            switch (e.getType()) {
                case CHILD_ADDED:
                    assignments.add(assignment);
                    break;
                case CHILD_REMOVED:
                    assignments.remove(assignment);
                    break;
            }
        });
    }

    public void start() throws Exception {
        cache.start();
    }

    public void stop() throws Exception {
        cache.stop();
    }

    public void registerAssignementCallback(SubscriptionAssignmentAware callback) {
        cache.registerCallback(ASSIGNMENT_LEVEL, (e) -> {
            SubscriptionName subscriptionName = pathSerializer.deserialize(e.getData().getPath()).getSubscriptionName();
            switch (e.getType()) {
                case CHILD_ADDED:
                    callback.onSubscriptionAssigned(subscriptionsCache.getSubscription(subscriptionName));
                    break;
                case CHILD_REMOVED:
                    callback.onAssignmentRemoved(subscriptionName);
                    break;
            }
        });
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

    public void removeSubscriptionEntry(SubscriptionName subscriptionName) {
        askCuratorPolitely(() -> curator.delete().guaranteed().forPath(pathSerializer.serialize(subscriptionName)));
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
