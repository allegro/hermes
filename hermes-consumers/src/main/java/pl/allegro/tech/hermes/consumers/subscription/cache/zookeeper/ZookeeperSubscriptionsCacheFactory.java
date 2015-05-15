package pl.allegro.tech.hermes.consumers.subscription.cache.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;

import javax.inject.Inject;
import javax.inject.Named;

public class ZookeeperSubscriptionsCacheFactory implements Factory<SubscriptionsCache> {

    private final CuratorFramework curatorClient;
    private final ConfigFactory configFactory;
    private final ObjectMapper objectMapper;

    @Inject
    public ZookeeperSubscriptionsCacheFactory(
            @Named(CuratorType.HERMES) CuratorFramework curatorClient,
            ConfigFactory configFactory,
            ObjectMapper objectMapper) {

        this.curatorClient = curatorClient;
        this.configFactory = configFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public SubscriptionsCache provide() {
        return new ZookeeperSubscriptionsCache(curatorClient, configFactory, objectMapper);
    }

    @Override
    public void dispose(SubscriptionsCache instance) {
        instance.stop();
    }
}
