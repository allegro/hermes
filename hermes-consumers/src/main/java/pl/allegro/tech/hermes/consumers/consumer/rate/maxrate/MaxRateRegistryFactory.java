package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;

import static org.slf4j.LoggerFactory.getLogger;
import static pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.ConsumerMaxRateRegistryType.FLAT_BINARY;
import static pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.ConsumerMaxRateRegistryType.HIERARCHICAL;

public class MaxRateRegistryFactory implements Factory<MaxRateRegistry> {

    private static final Logger logger = getLogger(MaxRateRegistryFactory.class);

    private final ConfigFactory configFactory;
    private final CuratorFramework curator;
    private final ObjectMapper objectMapper;
    private final ZookeeperPaths zookeeperPaths;
    private final MaxRatePathSerializer pathSerializer;
    private final SubscriptionsCache subscriptionCache;

    @Inject
    public MaxRateRegistryFactory(ConfigFactory configFactory, CuratorFramework curator, ObjectMapper objectMapper,
                                  ZookeeperPaths zookeeperPaths, MaxRatePathSerializer pathSerializer, SubscriptionsCache subscriptionCache) {
        this.configFactory = configFactory;
        this.curator = curator;
        this.objectMapper = objectMapper;
        this.zookeeperPaths = zookeeperPaths;
        this.pathSerializer = pathSerializer;
        this.subscriptionCache = subscriptionCache;
    }

    @Override
    public MaxRateRegistry provide() {
        String strategy = configFactory.getStringProperty(Configs.CONSUMER_MAXRATE_REGISTRY_TYPE);
        logger.info("Max rate provider strategy chosen: {}", strategy);

        switch (strategy) {
            case HIERARCHICAL:
                return new HierarchicalCacheMaxRateRegistry(configFactory, curator, objectMapper, zookeeperPaths, pathSerializer, subscriptionCache);
            case FLAT_BINARY:
                throw new IllegalStateException("Not yet implemented");
            default:
                throw new ConsumerMaxRateRegistryType.UnknownMaxRateRegistryException();
        }

    }

    @Override
    public void dispose(MaxRateRegistry instance) {

    }
}
