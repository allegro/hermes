package pl.allegro.tech.hermes.common.di.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperWorkloadConstraintsRepository;

public class WorkloadConstraintsRepositoryFactory {

    private final CuratorFramework curator;
    private final ObjectMapper mapper;
    private final ZookeeperPaths paths;

    public WorkloadConstraintsRepositoryFactory(CuratorFramework curator,
                                                ObjectMapper mapper,
                                                ZookeeperPaths paths) {
        this.curator = curator;
        this.mapper = mapper;
        this.paths = paths;
    }

    public WorkloadConstraintsRepository provide() {
        return new ZookeeperWorkloadConstraintsRepository(curator, mapper, paths);
    }
}
