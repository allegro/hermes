package pl.allegro.tech.hermes.consumers.subscription.id;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;

public class SubscriptionIdProviderFactory implements Factory<SubscriptionIdProvider> {

    private final CuratorFramework curatorFramework;
    private final ZookeeperPaths zookeeperPaths;

    @Inject
    public SubscriptionIdProviderFactory(CuratorFramework curatorFramework, ZookeeperPaths zookeeperPaths) {
        this.curatorFramework = curatorFramework;
        this.zookeeperPaths = zookeeperPaths;
    }

    @Override
    public SubscriptionIdProvider provide() {
        return new ZookeeperSubscriptionIdProvider(curatorFramework, zookeeperPaths);
    }

    @Override
    public void dispose(SubscriptionIdProvider instance) {
    }
}
