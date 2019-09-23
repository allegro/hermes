package pl.allegro.tech.hermes.common.di.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperWorkloadConstraintsRepository;

import javax.inject.Inject;
import javax.inject.Named;

public class WorkloadConstraintsRepositoryFactory implements Factory<WorkloadConstraintsRepository> {

    private final CuratorFramework curator;
    private final ObjectMapper mapper;
    private final ZookeeperPaths paths;

    @Inject
    public WorkloadConstraintsRepositoryFactory(@Named(CuratorType.HERMES) CuratorFramework curator,
                                                ObjectMapper mapper,
                                                ZookeeperPaths paths) {
        this.curator = curator;
        this.mapper = mapper;
        this.paths = paths;
    }

    @Override
    public WorkloadConstraintsRepository provide() {
        return new ZookeeperWorkloadConstraintsRepository(curator, mapper, paths);
    }

    @Override
    public void dispose(WorkloadConstraintsRepository instance) {
    }
}
