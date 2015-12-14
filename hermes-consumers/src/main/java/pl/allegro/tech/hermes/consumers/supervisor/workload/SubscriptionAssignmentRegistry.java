package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.cache.zookeeper.StartableCache;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

import java.util.concurrent.ExecutorService;

public class SubscriptionAssignmentRegistry extends StartableCache<SubscriptionAssignmentAware> implements PathChildrenCacheListener {

    private final SubscriptionRepository subscriptionRepository;
    private final String consumerNodeId;
    private final SubscriptionAssignmentPathSerializer pathSerializer;

    public SubscriptionAssignmentRegistry(CuratorFramework curatorClient,
                                          String path,
                                          ExecutorService executorService,
                                          SubscriptionRepository subscriptionRepository,
                                          String consumerNodeId,
                                          SubscriptionAssignmentPathSerializer pathSerializer) {
        super(curatorClient, path, executorService);
        this.subscriptionRepository = subscriptionRepository;
        this.consumerNodeId = consumerNodeId;
        this.pathSerializer = pathSerializer;
        getListenable().addListener(this);
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        if (event.getData() == null || event.getData().getPath() == null) {
            return;
        }
        SubscriptionAssignment path = pathSerializer.deserialize(event.getData().getPath());
        if (this.consumerNodeId.equals(path.getConsumerNodeId())) {
            switch (event.getType()) {
                case CHILD_ADDED:
                    Subscription subscription = subscriptionRepository.getSubscriptionDetails(path.getSubscriptionName());
                    for (SubscriptionAssignmentAware callback : callbacks) {
                        callback.onSubscriptionAssigned(subscription);
                    }
                    break;
                case CHILD_REMOVED:
                    for (SubscriptionAssignmentAware callback : callbacks) {
                        callback.onAssignmentRemoved(path.getSubscriptionName());
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
