package pl.allegro.tech.hermes.consumers.supervisor.workload;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;

import static org.slf4j.LoggerFactory.getLogger;

public class SubscriptionAssignmentNotifyingRepositoryFactory implements Factory<SubscriptionAssignmentNotifyingCache> {

    private static final Logger logger = getLogger(SubscriptionAssignmentNotifyingRepositoryFactory.class);

    private final CuratorFramework curator;
    private final ConfigFactory configFactory;
    private final ZookeeperPaths zookeeperPaths;
    private final SubscriptionsCache subscriptionsCache;

    @Inject
    public SubscriptionAssignmentNotifyingRepositoryFactory(
            CuratorFramework curator,
            ConfigFactory configFactory,
            ZookeeperPaths zookeeperPaths,
            SubscriptionsCache subscriptionsCache
    ) {
        this.curator = curator;
        this.configFactory = configFactory;
        this.zookeeperPaths = zookeeperPaths;
        this.subscriptionsCache = subscriptionsCache;
    }

    @Override
    public SubscriptionAssignmentNotifyingCache provide() {
        ConsumerWorkloadRegistryType type;
        try {
            String typeString = configFactory.getStringProperty(Configs.CONSUMER_WORKLOAD_REGISTRY_TYPE);
            type = ConsumerWorkloadRegistryType.fromString(typeString);
        } catch (Exception e) {
            logger.error("Could not configure subscription assignment notifying repository based on specified consumer workload registry type", e);
            throw e;
        }

        switch (type) {
            case HIERARCHICAL:
                return new HierarchicalSubscriptionAssignmentCache(curator, configFactory, zookeeperPaths, subscriptionsCache);
            case FLAT_BINARY:
                throw new UnsupportedOperationException("Not implemented yet");
            default:
                throw new UnsupportedOperationException("Max-rate type not supported.");
        }
    }

    @Override
    public void dispose(SubscriptionAssignmentNotifyingCache instance) {

    }
}
