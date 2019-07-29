package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.HierarchicalCache;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;
import static pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentRegistry.AUTO_ASSIGNED_MARKER;

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

    @Inject
    public SubscriptionAssignmentCache(CuratorFramework curator,
                                       ConfigFactory configFactory,
                                       ZookeeperPaths zookeeperPaths,
                                       SubscriptionsCache subscriptionsCache) {
        this.curator = curator;
        this.basePath = zookeeperPaths.consumersRuntimePath(configFactory.getStringProperty(KAFKA_CLUSTER_NAME));
        this.subscriptionsCache = subscriptionsCache;
        this.pathSerializer = new SubscriptionAssignmentPathSerializer(basePath, AUTO_ASSIGNED_MARKER);
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("subscription-assignment-cache-%d").build();
        this.cache = new HierarchicalCache(
                curator, Executors.newSingleThreadScheduledExecutor(threadFactory), basePath, 2, emptyList(), false
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
        long startNanos = System.nanoTime();

        logger.info("Starting assignment cache for {}", basePath);

        List<SubscriptionAssignment> currentAssignments = readExistingAssignments();
        currentAssignments.forEach(this::onAssignmentAdded);

        cache.start();

        started = true;

        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

        logger.info("Started assignment cache for {}. Read {} assignments. Took {}ms",
                basePath, currentAssignments.size(), elapsedMillis);
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

    public Map<SubscriptionName, Set<String>> getSubscriptionConsumers() {
        return createSnapshot().getAllAssignments().stream()
                .collect(Collectors.groupingBy(
                        SubscriptionAssignment::getSubscriptionName,
                        Collectors.mapping(SubscriptionAssignment::getConsumerNodeId, Collectors.toSet())));
    }
}
