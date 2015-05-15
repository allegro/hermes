package pl.allegro.tech.hermes.common.di.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository;

import javax.inject.Inject;
import javax.inject.Named;

public class TopicRepositoryFactory implements Factory<TopicRepository> {

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final ObjectMapper mapper;

    private final GroupRepository groupRepository;

    @Inject
    public TopicRepositoryFactory(@Named(CuratorType.HERMES) CuratorFramework zookeeper, ZookeeperPaths paths,
                                  ObjectMapper mapper, GroupRepository groupRepository) {
        this.zookeeper = zookeeper;
        this.paths = paths;
        this.mapper = mapper;
        this.groupRepository = groupRepository;
    }

    @Override
    public TopicRepository provide() {
        return new ZookeeperTopicRepository(zookeeper, mapper, paths, groupRepository);
    }

    @Override
    public void dispose(TopicRepository instance) {
    }
}
