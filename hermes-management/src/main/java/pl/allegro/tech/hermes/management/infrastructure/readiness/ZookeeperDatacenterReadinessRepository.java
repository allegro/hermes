package pl.allegro.tech.hermes.management.infrastructure.readiness;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import pl.allegro.tech.hermes.api.Readiness;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperBasedRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.readiness.ReadinessRepository;

public class ZookeeperDatacenterReadinessRepository extends ZookeeperBasedRepository implements ReadinessRepository {

    public ZookeeperDatacenterReadinessRepository(CuratorFramework curator, ObjectMapper mapper, ZookeeperPaths paths) {
        super(curator, mapper, paths);
    }

    @Override
    public boolean isReady() {
        try {
            String path = paths.frontendReadinessPath();
            Readiness readiness = readFrom(path, Readiness.class);
            return readiness.isReady();
        } catch (InternalProcessingException e) {
            if (e.getCause() instanceof KeeperException.NoNodeException) {
                return true;
            }
            throw e;
        }
    }

    @Override
    public void setReadiness(boolean isReady) {
        try {
            String path = paths.frontendReadinessPath();
            if (!pathExists(path)) {
                createRecursively(path, new Readiness(isReady));
            } else {
                overwrite(path, new Readiness(isReady));
            }
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }
}
