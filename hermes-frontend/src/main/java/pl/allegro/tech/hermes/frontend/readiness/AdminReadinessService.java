package pl.allegro.tech.hermes.frontend.readiness;

import static pl.allegro.tech.hermes.api.DatacenterReadiness.ReadinessStatus.READY;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.DatacenterReadiness;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.readiness.DatacenterReadinessList;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public class AdminReadinessService implements NodeCacheListener {

  private static final Logger logger = LoggerFactory.getLogger(AdminReadinessService.class);

  private final NodeCache cache;
  private final ObjectMapper mapper;
  private final String localDatacenterName;

  private volatile Map<String, Boolean> readinessPerDatacenter;

  public AdminReadinessService(
      ObjectMapper mapper,
      CuratorFramework curator,
      ZookeeperPaths paths,
      String localDatacenterName) {
    this.mapper = mapper;
    this.localDatacenterName = localDatacenterName;
    this.cache = new NodeCache(curator, paths.datacenterReadinessPath());
    cache.getListenable().addListener(this);
    try {
      cache.start(true);
    } catch (Exception e) {
      throw new InternalProcessingException("Readiness cache cannot start.", e);
    }
  }

  public void start() {
    refreshAdminReady();
    logger.info("Initial readiness per datacenter: {}", readinessPerDatacenter);
  }

  public void stop() {
    try {
      cache.close();
    } catch (Exception e) {
      logger.warn("Failed to stop readiness cache", e);
    }
  }

  @Override
  public void nodeChanged() {
    refreshAdminReady();
    logger.info("Readiness per datacenter changed to: {}", readinessPerDatacenter);
  }

  private void refreshAdminReady() {
    try {
      ChildData nodeData = cache.getCurrentData();
      if (nodeData != null) {
        byte[] data = nodeData.getData();
        DatacenterReadinessList readiness = mapper.readValue(data, DatacenterReadinessList.class);
        readinessPerDatacenter =
            readiness.datacenters().stream()
                .collect(
                    Collectors.toMap(
                        DatacenterReadiness::getDatacenter, e -> e.getStatus() == READY));
      } else {
        readinessPerDatacenter = Collections.emptyMap();
      }
    } catch (Exception e) {
      logger.error("Failed reloading readiness cache.", e);
    }
  }

  public boolean isLocalDatacenterReady() {
    return isDatacenterReady(localDatacenterName);
  }

  public boolean isDatacenterReady(String datacenter) {
    return readinessPerDatacenter != null && readinessPerDatacenter.getOrDefault(datacenter, true);
  }
}
