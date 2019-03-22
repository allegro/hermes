package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

public class SubscriptionAssignmentRegistry implements SubscriptionAssignmentAware {

    private static final Logger logger = getLogger(SubscriptionAssignmentRegistry.class);

    public static final byte[] AUTO_ASSIGNED_MARKER = "AUTO_ASSIGNED".getBytes();

    private final CuratorFramework curator;

    private final SubscriptionAssignmentPathSerializer pathSerializer;
    private final SubscriptionAssignmentCache subscriptionAssignmentCache;

    public SubscriptionAssignmentRegistry(CuratorFramework curator,
                                          SubscriptionAssignmentCache subscriptionAssignmentCache,
                                          SubscriptionAssignmentPathSerializer pathSerializer) {
        this.curator = curator;
        this.pathSerializer = pathSerializer;
        this.subscriptionAssignmentCache = subscriptionAssignmentCache;
    }

    public void start() throws Exception {
        subscriptionAssignmentCache.registerAssignmentCallback(this);
    }

    public boolean isStarted() {
        return subscriptionAssignmentCache.isStarted();
    }

    @Override
    public void onSubscriptionAssigned(SubscriptionName subscriptionName) {}

    @Override
    public void onAssignmentRemoved(SubscriptionName subscriptionName) {
        removeSubscriptionEntryIfEmpty(subscriptionName);
    }

    @Override
    public Optional<String> watchedConsumerId() {
        return Optional.empty();
    }

    public void registerAssignmentCallback(SubscriptionAssignmentAware callback) {
        subscriptionAssignmentCache.registerAssignmentCallback(callback);
    }

    boolean isAssignedTo(String nodeId, SubscriptionName subscription) {
        return subscriptionAssignmentCache.isAssignedTo(nodeId, subscription);
    }

    public SubscriptionAssignmentView createSnapshot() {
        return subscriptionAssignmentCache.createSnapshot();
    }

    void dropAssignment(SubscriptionAssignment assignment) {
        String message = String.format("Dropping assignment [consumer=%s, subscription=%s]",
                assignment.getConsumerNodeId(), assignment.getSubscriptionName().getQualifiedName());
        logger.info(message);
        askCuratorPolitely(() -> curator.delete().guaranteed().forPath(
                pathSerializer.serialize(assignment.getSubscriptionName(), assignment.getConsumerNodeId())), message);
    }

    void addPersistentAssignment(SubscriptionAssignment assignment) {
        addAssignment(assignment, CreateMode.PERSISTENT);
    }

    void addEphemeralAssignment(SubscriptionAssignment assignment) {
        addAssignment(assignment, CreateMode.EPHEMERAL);
    }

    private void removeSubscriptionEntryIfEmpty(SubscriptionName subscriptionName) {
        String message = String.format("Removing empty assignment node [subscription=%s]", subscriptionName.getQualifiedName());
        askCuratorPolitely(() -> {
            if (curator.getChildren().forPath(pathSerializer.serialize(subscriptionName)).isEmpty()) {
                logger.info(message);
                curator.delete().guaranteed().forPath(pathSerializer.serialize(subscriptionName));
            }
        }, message);
    }

    private void addAssignment(SubscriptionAssignment assignment, CreateMode createMode) {
        String message = String.format("Adding assignment [consumer=%s, subscription=%s]",
                assignment.getConsumerNodeId(), assignment.getSubscriptionName().getQualifiedName());
        logger.info(message);
        askCuratorPolitely(() -> {
            String path = pathSerializer.serialize(assignment.getSubscriptionName(), assignment.getConsumerNodeId());
            curator.create().creatingParentsIfNeeded().withMode(createMode)
                    .forPath(path, AUTO_ASSIGNED_MARKER);
        }, message);
    }

    interface CuratorTask {
        void run() throws Exception;
    }

    private void askCuratorPolitely(CuratorTask task, String description) {
        try {
            task.run();
        } catch (KeeperException.NodeExistsException | KeeperException.NoNodeException ex) {
            logger.warn("An error occurred while writing to assignment registry, ignoring. Action: {}", description, ex);
            // ignore
        } catch (Exception ex) {
            logger.error("An error occurred while writing to assignment registry. Action: {}", description, ex);
            throw new InternalProcessingException(ex);
        }
    }

}
