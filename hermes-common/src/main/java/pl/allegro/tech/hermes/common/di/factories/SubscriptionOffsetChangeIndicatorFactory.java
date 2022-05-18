package pl.allegro.tech.hermes.common.di.factories;

import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionOffsetChangeIndicator;

public class SubscriptionOffsetChangeIndicatorFactory {

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionOffsetChangeIndicatorFactory(CuratorFramework zookeeper, ZookeeperPaths paths,
                                                    SubscriptionRepository subscriptionRepository) {
        this.zookeeper = zookeeper;
        this.paths = paths;
        this.subscriptionRepository = subscriptionRepository;
    }

    public SubscriptionOffsetChangeIndicator provide() {
        return new ZookeeperSubscriptionOffsetChangeIndicator(zookeeper, paths, subscriptionRepository);
    }
}
