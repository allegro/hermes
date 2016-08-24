package pl.allegro.tech.hermes.frontend.di;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.frontend.blacklist.BlacklistZookeeperNotifyingCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import javax.inject.Named;

public class BlacklistZookeeperNotifyingCacheFactory implements Factory<BlacklistZookeeperNotifyingCache> {

    private final CuratorFramework curator;
    private final ZookeeperPaths zookeeperPaths;

    @Inject
    public BlacklistZookeeperNotifyingCacheFactory(@Named(CuratorType.HERMES) CuratorFramework curator, ZookeeperPaths zookeeperPaths) {
        this.curator = curator;
        this.zookeeperPaths = zookeeperPaths;
    }

    @Override
    public BlacklistZookeeperNotifyingCache provide() {
        BlacklistZookeeperNotifyingCache cache = new BlacklistZookeeperNotifyingCache(curator, zookeeperPaths);
        try {
            cache.start();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start Zookeeper Topic Blacklist cache", e);
        }
        return cache;
    }

    @Override
    public void dispose(BlacklistZookeeperNotifyingCache instance) {

    }
}
