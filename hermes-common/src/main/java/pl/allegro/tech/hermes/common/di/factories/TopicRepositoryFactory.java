package pl.allegro.tech.hermes.common.di.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository;

public class TopicRepositoryFactory {

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final ObjectMapper mapper;

    private final GroupRepository groupRepository;

    public TopicRepositoryFactory(CuratorFramework zookeeper, ZookeeperPaths paths,
                                  ObjectMapper mapper, GroupRepository groupRepository) {
        this.zookeeper = zookeeper;
        this.paths = paths;
        this.mapper = mapper;
        this.groupRepository = groupRepository;
    }

    public TopicRepository provide() {
        return new ZookeeperTopicRepository(zookeeper, mapper, paths, groupRepository);
    }
}
