package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.HierarchicalCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class SubscriptionAssignmentCache {

    private static final int SUBSCRIPTION_LEVEL = 0;

    private static final int ASSIGNMENT_LEVEL = 1;

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionAssignmentCache.class);

    private final String basePath;

    private final CuratorFramework curator;

    private final Set<SubscriptionAssignment> assignments = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Set<SubscriptionAssignmentAware> callbacks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final HierarchicalCache cache;

    private final SubscriptionsCache subscriptionsCache;

    private final SubscriptionAssignmentPathSerializer pathSerializer;

    private volatile boolean started = false;

    public SubscriptionAssignmentCache(CuratorFramework curator,
                                       String basePath,
                                       SubscriptionsCache subscriptionsCache,
                                       SubscriptionAssignmentPathSerializer pathSerializer) {
        this.curator = curator;
        this.basePath = basePath;
        this.subscriptionsCache = subscriptionsCache;
        this.pathSerializer = pathSerializer;
        this.cache = new HierarchicalCache(
                curator, Executors.newSingleThreadScheduledExecutor(), basePath, 2, Collections.emptyList()
        );

        cache.registerCallback(ASSIGNMENT_LEVEL, (e) -> {
            SubscriptionAssignment assignment =
                    pathSerializer.deserialize(e.getData().getPath(), e.getData().getData());
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
        logger.info("Starting assignment cache for {}", basePath);

        List<SubscriptionAssignment> currentAssignments = readExistingAssignments();
        currentAssignments.forEach(this::onAssignmentAdded);

        cache.start();

        started = true;

        logger.info("Started assignment cache for {}. Read {} assignments", basePath, currentAssignments.size());
    }

    public void stop() throws Exception {
        logger.info("Stopping assignment cache for {}", basePath);

        cache.stop();

        logger.info("Stopped assignment cache for {}", basePath);
    }

    public boolean isStarted() {
        return started;
    }

    public SubscriptionAssignmentView createSnapshot() {
        return SubscriptionAssignmentView.of(assignments);
    }

    boolean isAssignedTo(String nodeId, SubscriptionName subscription) {
        return assignments.stream()
                .anyMatch(a -> a.getSubscriptionName().equals(subscription)
                        && a.getConsumerNodeId().equals(nodeId));
    }

    void registerAssignmentCallback(SubscriptionAssignmentAware callback) {
        callbacks.add(callback);
    }

    private List<SubscriptionAssignment> readExistingAssignments() {
        List<SubscriptionAssignment> existingAssignments = new ArrayList<>();

        for (SubscriptionName subscriptionName : subscriptionsCache.listActiveSubscriptionNames()) {
            try {
                String path = pathSerializer.serialize(subscriptionName);
                List<String> nodes = curator.getChildren().forPath(path);
                for (String node : nodes) {
                    String fullPath = path + "/" + node;
                    existingAssignments.add(
                            pathSerializer.deserialize(fullPath, curator.getData().forPath(fullPath)));
                }
            } catch (Exception e) {
                logger.info("Exception occurred when initializing cache with subscription {}", subscriptionName, e);
            }
        }
        return existingAssignments;
    }

    private void onAssignmentAdded(SubscriptionAssignment assignment) {
        try {
            if (assignments.add(assignment)) {
                callbacks.stream()
                        .filter(callback -> shouldNotify(callback, assignment))
                        .forEach(callback -> callback.onSubscriptionAssigned(assignment.getSubscriptionName()));
            }
        } catch (Exception e) {
            logger.error("Exception while adding assignment {}", assignment, e);
        }
    }

    private void onAssignmentRemoved(SubscriptionAssignment assignment) {
        try {
            if (assignments.remove(assignment)) {
                callbacks.stream()
                        .filter(callback -> shouldNotify(callback, assignment))
                        .forEach(callback -> callback.onAssignmentRemoved(assignment.getSubscriptionName()));
            }
        } catch (Exception e) {
            logger.error("Exception while removing assignment {}", assignment, e);
        }
    }

    private boolean shouldNotify(SubscriptionAssignmentAware callback, SubscriptionAssignment assignment) {
        return !callback.watchedConsumerId().isPresent()
                || callback.watchedConsumerId().get().equals(assignment.getConsumerNodeId());
    }


}
