package pl.allegro.tech.hermes.management.infrastructure.readiness;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import pl.allegro.tech.hermes.api.DatacenterReadiness;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.readiness.DatacenterReadinessList;
import pl.allegro.tech.hermes.infrastructure.MalformedDataException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperBasedRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.readiness.DatacenterReadinessRepository;

public class ZookeeperDatacenterReadinessRepository extends ZookeeperBasedRepository
    implements DatacenterReadinessRepository {

  public ZookeeperDatacenterReadinessRepository(
      CuratorFramework curator, ObjectMapper mapper, ZookeeperPaths paths) {
    super(curator, mapper, paths);
  }

  @Override
  public List<DatacenterReadiness> getReadiness() {
    try {
      String path = paths.datacenterReadinessPath();
      return readFrom(path, DatacenterReadinessList.class).datacenters();
    } catch (InternalProcessingException e) {
      if (e.getCause() instanceof KeeperException.NoNodeException) {
        return Collections.emptyList();
      }
      throw e;
    } catch (MalformedDataException e) {
      return Collections.emptyList();
    }
  }

  @Override
  public void setReadiness(List<DatacenterReadiness> datacenterReadiness) {
    try {
      String path = paths.datacenterReadinessPath();
      if (!pathExists(path)) {
        createRecursively(path, new DatacenterReadinessList(datacenterReadiness));
      } else {
        overwrite(path, new DatacenterReadinessList(datacenterReadiness));
      }
    } catch (Exception ex) {
      throw new InternalProcessingException(ex);
    }
  }
}
