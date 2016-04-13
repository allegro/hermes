package pl.allegro.tech.hermes.frontend.di.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.frontend.publishing.message.preview.PreviewMessageLog;
import pl.allegro.tech.hermes.frontend.publishing.message.preview.ZookeeperPreviewMessageLog;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import javax.inject.Named;

public class PreviewMessageLogFactory implements Factory<PreviewMessageLog> {

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final ObjectMapper mapper;

    @Inject
    public PreviewMessageLogFactory(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                    ZookeeperPaths paths, ObjectMapper mapper) {
        this.zookeeper = zookeeper;
        this.paths = paths;
        this.mapper = mapper;
    }

    @Override
    public PreviewMessageLog provide() {
        return new ZookeeperPreviewMessageLog(zookeeper, paths);
    }

    @Override
    public void dispose(PreviewMessageLog instance) {
    }
}

