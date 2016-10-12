package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;

public class SubscriptionConsumersCacheFactory implements Factory<SubscriptionConsumersCache> {

    private final CuratorFramework curator;
    private final ZookeeperPaths zookeeperPaths;
    private final SubscriptionsCache subscriptionsCache;

    @Inject
    public SubscriptionConsumersCacheFactory(CuratorFramework curator,
                                             ZookeeperPaths zookeeperPaths,
                                             SubscriptionsCache subscriptionsCache) {
        this.curator = curator;
        this.zookeeperPaths = zookeeperPaths;
        this.subscriptionsCache = subscriptionsCache;
    }

    @Override
    public SubscriptionConsumersCache provide() {
        return new SubscriptionConsumersCache(curator, zookeeperPaths, subscriptionsCache);
    }

    @Override
    public void dispose(SubscriptionConsumersCache instance) {
        instance.stop();
    }
}
