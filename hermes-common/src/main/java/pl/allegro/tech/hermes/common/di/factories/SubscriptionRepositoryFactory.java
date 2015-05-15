package pl.allegro.tech.hermes.common.di.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionRepository;

import javax.inject.Inject;
import javax.inject.Named;

public class SubscriptionRepositoryFactory implements Factory<SubscriptionRepository> {

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final ObjectMapper mapper;

    private final TopicRepository topicRepository;

    @Inject
    public SubscriptionRepositoryFactory(@Named(CuratorType.HERMES) CuratorFramework zookeeper, ZookeeperPaths paths,
                                         ObjectMapper mapper, TopicRepository topicRepository) {
        this.zookeeper = zookeeper;
        this.paths = paths;
        this.mapper = mapper;
        this.topicRepository = topicRepository;
    }

    @Override
    public SubscriptionRepository provide() {
        return new ZookeeperSubscriptionRepository(zookeeper, mapper, paths, topicRepository);
    }

    @Override
    public void dispose(SubscriptionRepository instance) {
    }
}
