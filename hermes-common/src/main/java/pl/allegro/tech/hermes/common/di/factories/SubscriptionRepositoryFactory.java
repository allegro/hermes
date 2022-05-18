package pl.allegro.tech.hermes.common.di.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionRepository;

public class SubscriptionRepositoryFactory {

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final ObjectMapper mapper;

    private final TopicRepository topicRepository;

    public SubscriptionRepositoryFactory(CuratorFramework zookeeper, ZookeeperPaths paths,
                                         ObjectMapper mapper, TopicRepository topicRepository) {
        this.zookeeper = zookeeper;
        this.paths = paths;
        this.mapper = mapper;
        this.topicRepository = topicRepository;
    }

    public SubscriptionRepository provide() {
        return new ZookeeperSubscriptionRepository(zookeeper, mapper, paths, topicRepository);
    }
}
