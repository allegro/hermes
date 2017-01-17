package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class SubscriptionAssignmentRegistry {

    private static final int SUBSCRIPTION_LEVEL = 0;

    private static final int ASSIGNMENT_LEVEL = 1;

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionAssignmentRegistry.class);

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
        logger.info("Starting assignment registry");

        List<SubscriptionAssignment> currentAssignments = readExistingAssignments();
        currentAssignments.forEach(this::onAssignmentAdded);

        cache.start();

        logger.info("Started assignment registry. Read {} assignments", currentAssignments.size());
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
        List<SubscriptionAssignment> existingAssignments = new ArrayList<>();

        for (SubscriptionName subscriptionName : subscriptionsCache.listActiveSubscriptionNames()) {
            try {
                String path = pathSerializer.serialize(subscriptionName);
                List<String> nodes = curator.getChildren().forPath(path);
                nodes.forEach(node -> existingAssignments.add(new SubscriptionAssignment(node, subscriptionName)));
            } catch (Exception e) {
                logger.info("Exception occurred when initializing cache with subscription {}", subscriptionName, e);
            }
        }
        return existingAssignments;
    }

    private void onAssignmentAdded(SubscriptionAssignment assignment) {
        try {
            if (assignments.add(assignment)) {
                if (consumerNodeId.equals(assignment.getConsumerNodeId())) {
                    callbacks.forEach(callback ->
                            callback.onSubscriptionAssigned(assignment.getSubscriptionName())
                    );
                }
            }
        } catch (Exception e) {
            logger.error("Exception while adding assignment {}", assignment, e);
        }
    }

    private void onAssignmentRemoved(SubscriptionAssignment assignment) {
        try {
            if (assignments.remove(assignment)) {
                if (consumerNodeId.equals(assignment.getConsumerNodeId())) {
                    callbacks.forEach(callback ->
                            callback.onAssignmentRemoved(assignment.getSubscriptionName())
                    );
                }
            }
            removeSubscriptionEntryIfEmpty(assignment.getSubscriptionName());
        } catch (Exception e) {
            logger.error("Exception while removing assignment {}", assignment, e);
        }
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
