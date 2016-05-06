package pl.allegro.tech.hermes.common.di.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperMessagePreviewRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import javax.inject.Named;

public class MessagePreviewRepositoryFactory implements Factory<MessagePreviewRepository> {

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final ObjectMapper mapper;

    @Inject
    public MessagePreviewRepositoryFactory(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                    ZookeeperPaths paths, ObjectMapper mapper) {
        this.zookeeper = zookeeper;
        this.paths = paths;
        this.mapper = mapper;
    }

    @Override
    public MessagePreviewRepository provide() {
        return new ZookeeperMessagePreviewRepository(zookeeper, mapper, paths);
    }

    @Override
    public void dispose(MessagePreviewRepository instance) {
    }
}
