package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.cache.zookeeper.NodeCache;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.apache.zookeeper.CreateMode.EPHEMERAL;
import static org.apache.zookeeper.CreateMode.PERSISTENT;

public class WorkTracker extends NodeCache<SubscriptionAssignmentAware, SubscriptionAssignmentRegistry> {
    private final SubscriptionRepository subscriptionRepository;
    private final String consumerNodeId;
    private final SubscriptionAssignmentPathSerializer pathSerializer;

public WorkTracker(CuratorFramework curatorClient,
                       ObjectMapper objectMapper,
                       String path,
                       String consumerNodeId,
                       ExecutorService executorService,
                       SubscriptionRepository subscriptionRepository) {
        super(curatorClient, objectMapper, path, executorService);
        this.subscriptionRepository = subscriptionRepository;
        this.consumerNodeId = consumerNodeId;
        this.pathSerializer = new SubscriptionAssignmentPathSerializer(path);
    }

    public void forceAssignment(Subscription subscription) {
        askCuratorPolitely(() -> curatorClient.create().creatingParentsIfNeeded().withMode(EPHEMERAL).forPath(pathSerializer.serialize(subscription.toSubscriptionName(), consumerNodeId)));
    }

    public void dropAssignment(Subscription subscription) {
        askCuratorPolitely(() -> curatorClient.delete().guaranteed().forPath(pathSerializer.serialize(subscription.toSubscriptionName(), consumerNodeId)));
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

    public WorkDistributionChanges apply(SubscriptionAssignmentView targetView) {
        SubscriptionAssignmentView currentView = getAssignments();

        SubscriptionAssignmentView deletionsView = currentView.deletions(targetView);
        Set<SubscriptionName> subscriptionsWithoutAssignments = deletionsView.getSubscriptionsWithoutAssignments();
        List<SubscriptionAssignment> assignmentDeletions = deletionsView.getAllAssignments();

        SubscriptionAssignmentView additionsView = currentView.additions(targetView);
        Set<SubscriptionName> subscriptionAdditions = additionsView.getSubscriptions();
        List<SubscriptionAssignment> assignmentAdditions = additionsView.getAllAssignments();

        assignmentDeletions.forEach(this::dropAssignment);
        assignmentAdditions.forEach(this::addAssignment);

        Sets.SetView<SubscriptionName> invalidSubscriptions = Sets.difference(subscriptionsWithoutAssignments, subscriptionAdditions);
        invalidSubscriptions.forEach(this::removeSubscriptionEntry);

        return new WorkDistributionChanges(assignmentDeletions.size(), assignmentAdditions.size(), invalidSubscriptions.size());
     }

    private void removeSubscriptionEntry(SubscriptionName subscriptionName) {
        askCuratorPolitely(() -> curatorClient.delete().guaranteed().forPath(pathSerializer.serialize(subscriptionName)));
    }

    private void dropAssignment(SubscriptionAssignment assignment) {
        askCuratorPolitely(() -> curatorClient.delete().guaranteed().forPath(pathSerializer.serialize(assignment.getSubscriptionName(), assignment.getConsumerNodeId())));
    }

    private void addAssignment(SubscriptionAssignment assignment) {
        askCuratorPolitely(() -> curatorClient.create().creatingParentsIfNeeded().withMode(PERSISTENT).forPath(pathSerializer.serialize(assignment.getSubscriptionName(), assignment.getConsumerNodeId())));
    }

    private Set<SubscriptionAssignment> getAssignments(String subscriptionName) {
        return getEntry(subscriptionName).getCurrentData().stream()
                .map(child -> pathSerializer.deserialize(child.getPath())).collect(Collectors.toSet());
    }

    public SubscriptionAssignmentView getAssignments() {
        return new SubscriptionAssignmentView(getSubcacheKeySet().stream().collect(toMap(SubscriptionName::fromString, this::getAssignments)));
    }

    public boolean isAssignedTo(SubscriptionName subscription, String consumerNodeId) {
        return getAssignments(subscription.toString()).stream().filter(assignment ->
                Objects.equals(assignment.getConsumerNodeId(), consumerNodeId)).findAny().isPresent();
    }

    interface CuratorTask {
        void run() throws Exception;
    }

    @Override
    protected SubscriptionAssignmentRegistry createSubcache(String path) {
        return new SubscriptionAssignmentRegistry(
                curatorClient,
                path,
                executorService,
                subscriptionRepository,
                consumerNodeId,
                pathSerializer);
    }

    public static class WorkDistributionChanges {
        private final int assignmentsDeleted;
        private final int assignmentsCreated;
        private final int invalidSubscriptionsDeleted;

        public WorkDistributionChanges(int assignmentsDeleted, int assignmentsCreated, int invalidSubscriptionsDeleted) {
            this.assignmentsDeleted = assignmentsDeleted;
            this.assignmentsCreated = assignmentsCreated;
            this.invalidSubscriptionsDeleted = invalidSubscriptionsDeleted;
        }

        public int getDeletedAssignmentsCount() {
            return assignmentsDeleted;
        }

        public int getCreatedAssignmentsCount() {
            return assignmentsCreated;
        }

        public String toString() {
            return format("assignments_created=%d, assignments_deleted=%d, invalid_subscriptions_deleted=%d",
                    assignmentsCreated, assignmentsDeleted, invalidSubscriptionsDeleted);
        }
    }
}
