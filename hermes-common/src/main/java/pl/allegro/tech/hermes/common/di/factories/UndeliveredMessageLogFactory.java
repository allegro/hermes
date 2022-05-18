package pl.allegro.tech.hermes.common.di.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.message.undelivered.ZookeeperUndeliveredMessageLog;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public class UndeliveredMessageLogFactory {

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final ObjectMapper mapper;

    public UndeliveredMessageLogFactory(CuratorFramework zookeeper,
                                        ZookeeperPaths paths, ObjectMapper mapper) {
        this.zookeeper = zookeeper;
        this.paths = paths;
        this.mapper = mapper;
    }

    public UndeliveredMessageLog provide() {
        return new ZookeeperUndeliveredMessageLog(zookeeper, paths, mapper);
    }
}
