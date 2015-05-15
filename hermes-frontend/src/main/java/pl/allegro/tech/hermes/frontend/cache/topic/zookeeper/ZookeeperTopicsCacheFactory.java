package pl.allegro.tech.hermes.frontend.cache.topic.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;

import javax.inject.Inject;
import javax.inject.Named;

public class ZookeeperTopicsCacheFactory implements Factory<TopicsCache> {

    private final CuratorFramework curatorClient;
    private final ConfigFactory configFactory;
    private final ObjectMapper objectMapper;

    @Inject
    public ZookeeperTopicsCacheFactory(
            @Named(CuratorType.HERMES) CuratorFramework curatorClient,
            ConfigFactory configFactory,
            ObjectMapper objectMapper) {

        this.curatorClient = curatorClient;
        this.configFactory = configFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public TopicsCache provide() {
        return new ZookeeperTopicsCache(curatorClient, configFactory, objectMapper);
    }

    @Override
    public void dispose(TopicsCache instance) {
        instance.stop();
    }
}
