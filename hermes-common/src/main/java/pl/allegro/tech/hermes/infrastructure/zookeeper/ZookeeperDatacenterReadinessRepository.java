package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.Readiness;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;

public class ZookeeperDatacenterReadinessRepository extends ZookeeperBasedRepository implements ReadinessRepository {

    public ZookeeperDatacenterReadinessRepository(CuratorFramework zookeeper,
                                                  ObjectMapper mapper,
                                                  ZookeeperPaths paths) {
        super(zookeeper, mapper, paths);
    }

    @Override
    public boolean isReady() {
        if (!pathExists(paths.frontendReadinessPath())) {
            return true;
        }
        return readFrom(paths.frontendReadinessPath(), Readiness.class).isReady();
    }

    @Override
    public void setReadiness(boolean isReady) {
        try {
            String path = paths.frontendReadinessPath();
            if (!pathExists(path)) {
                zookeeper.create()
                        .creatingParentsIfNeeded()
                        .forPath(path, mapper.writeValueAsBytes(new Readiness(isReady)));
            } else {
                zookeeper.setData().forPath(path, mapper.writeValueAsBytes(new Readiness(isReady)));
            }
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }
}
