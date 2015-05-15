package pl.allegro.tech.hermes.common.di.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.message.undelivered.ZookeeperUndeliveredMessageLog;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import javax.inject.Named;

public class UndeliveredMessageLogFactory implements Factory<UndeliveredMessageLog> {

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final ObjectMapper mapper;

    @Inject
    public UndeliveredMessageLogFactory(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                        ZookeeperPaths paths, ObjectMapper mapper) {
        this.zookeeper = zookeeper;
        this.paths = paths;
        this.mapper = mapper;
    }

    @Override
    public UndeliveredMessageLog provide() {
        return new ZookeeperUndeliveredMessageLog(zookeeper, paths, mapper);
    }

    @Override
    public void dispose(UndeliveredMessageLog instance) {
    }
}
