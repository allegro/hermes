package pl.allegro.tech.hermes.consumers.supervisor.workload;

import com.fasterxml.jackson.databind.ObjectMapper;
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

        List<SubscriptionAssignment> deletions = currentView.deletions(targetView).getAllAssignments();
        List<SubscriptionAssignment> additions = currentView.additions(targetView).getAllAssignments();

        deletions.forEach(this::dropAssignment);
        additions.forEach(this::addAssignment);

        return new WorkDistributionChanges(deletions.size(), additions.size());
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
        private final int deleted;
        private final int created;

        public WorkDistributionChanges(int deleted, int created) {
            this.deleted = deleted;
            this.created = created;
        }

        public int getDeletedAssignmentsCount() {
            return deleted;
        }

        public int getCreatedAssignmentsCount() {
            return created;
        }

        public String toString() {
            return format("assignments_created=%d, assignments_deleted=%d", created, deleted);
        }
    }
}
