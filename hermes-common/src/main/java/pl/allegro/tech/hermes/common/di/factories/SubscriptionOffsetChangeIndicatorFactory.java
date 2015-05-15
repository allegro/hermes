package pl.allegro.tech.hermes.common.di.factories;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.subscription.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionOffsetChangeIndicator;

import javax.inject.Inject;

public class SubscriptionOffsetChangeIndicatorFactory implements Factory<SubscriptionOffsetChangeIndicator> {

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final SubscriptionRepository subscriptionRepository;

    @Inject
    public SubscriptionOffsetChangeIndicatorFactory(CuratorFramework zookeeper, ZookeeperPaths paths,
                                                    SubscriptionRepository subscriptionRepository) {
        this.zookeeper = zookeeper;
        this.paths = paths;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public SubscriptionOffsetChangeIndicator provide() {
        return new ZookeeperSubscriptionOffsetChangeIndicator(zookeeper, paths, subscriptionRepository);
    }

    @Override
    public void dispose(SubscriptionOffsetChangeIndicator instance) {

    }
}
