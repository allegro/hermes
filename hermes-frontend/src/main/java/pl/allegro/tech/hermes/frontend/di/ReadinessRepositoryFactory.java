package pl.allegro.tech.hermes.frontend.di;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperDatacenterReadinessRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import javax.inject.Named;

public class ReadinessRepositoryFactory implements Factory<ReadinessRepository> {
    private final CuratorFramework zookeeper;
    private final ZookeeperPaths paths;
    private final ObjectMapper mapper;

    @Inject
    public ReadinessRepositoryFactory(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                      ZookeeperPaths paths,
                                      ObjectMapper mapper) {
        this.zookeeper = zookeeper;
        this.paths = paths;
        this.mapper = mapper;
    }

    @Override
    public ReadinessRepository provide() {
        return new ZookeeperDatacenterReadinessRepository(zookeeper, mapper, paths);
    }

    @Override
    public void dispose(ReadinessRepository instance) {
        instance.close();
    }
}
