package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.workload.constraints.ConsumersWorkloadConstraints;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;

public class ZookeeperWorkloadConstraintsRepository extends ZookeeperBasedRepository implements WorkloadConstraintsRepository {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperWorkloadConstraintsRepository.class);

    private final ZookeeperWorkloadConstraintsCache pathChildrenCache;

    public ZookeeperWorkloadConstraintsRepository(CuratorFramework curator, ObjectMapper mapper, ZookeeperPaths paths) {
        this(curator, mapper, paths, new ZookeeperWorkloadConstraintsCache(curator, mapper, paths));
    }

    ZookeeperWorkloadConstraintsRepository(CuratorFramework curator, ObjectMapper mapper, ZookeeperPaths paths,
                                           ZookeeperWorkloadConstraintsCache pathChildrenCache) {
        super(curator, mapper, paths);
        this.pathChildrenCache = pathChildrenCache;
        try {
            this.pathChildrenCache.start();
        } catch (Exception e) {
            throw new InternalProcessingException("ZookeeperWorkloadConstraintsCache cannot start.", e);
        }
    }

    @Override
    public ConsumersWorkloadConstraints getConsumersWorkloadConstraints() {
        return pathChildrenCache.getConsumersWorkloadConstraints();
    }
}
