package pl.allegro.tech.hermes.common.di.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.util.InstanceIdResolver;
import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperDatacenterReadinessRepository;


public class ReadinessRepositoryFactory implements Factory<List<ReadinessRepository>> {

    private final String hostname;

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final ObjectMapper mapper;

    @Inject
    public ReadinessRepositoryFactory(@Named(CuratorType.HERMES) CuratorFramework zookeeper, ZookeeperPaths paths,
                                      ObjectMapper mapper, InstanceIdResolver instanceIdResolver) {
        this.zookeeper = zookeeper;
        this.paths = paths;
        this.mapper = mapper;
        this.hostname = instanceIdResolver.resolve();
    }

    @Override
    public List<ReadinessRepository> provide() {
        return Lists.newArrayList(new ZookeeperDatacenterReadinessRepository(zookeeper, mapper, paths, hostname));
    }

    @Override
    public void dispose(List<ReadinessRepository> instances) {
    }
}
