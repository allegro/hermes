package pl.allegro.tech.hermes.management.infrastructure.detection;

import jakarta.annotation.PostConstruct;
import java.util.Optional;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.config.detection.InactiveTopicsDetectionProperties;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager;

public class InactiveTopicsDetectionLeader {

  private final String leaderElectionDc;
  private final Optional<LeaderLatch> leaderLatch;

  private static final Logger logger = LoggerFactory.getLogger(InactiveTopicsDetectionLeader.class);

  public InactiveTopicsDetectionLeader(
      ZookeeperClientManager zookeeperClientManager,
      InactiveTopicsDetectionProperties inactiveTopicsDetectionProperties,
      ZookeeperPaths zookeeperPaths) {
    this.leaderElectionDc = inactiveTopicsDetectionProperties.leaderElectionZookeeperDc();
    Optional<CuratorFramework> leaderCuratorFramework =
        zookeeperClientManager.getClients().stream()
            .filter(it -> it.getDatacenterName().equals(leaderElectionDc))
            .findFirst()
            .map(ZookeeperClient::getCuratorFramework);
    String leaderPath = zookeeperPaths.inactiveTopicsLeaderPath();
    this.leaderLatch = leaderCuratorFramework.map(it -> new LeaderLatch(it, leaderPath));
  }

  @PostConstruct
  public void startListeningForLeadership() {
    if (leaderLatch.isPresent()) {
      logger.info("Starting listening for leadership");
      try {
        leaderLatch.get().start();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      logLeaderZookeeperClientNotFound(leaderElectionDc);
    }
  }

  public boolean isLeader() {
    return leaderLatch.map(LeaderLatch::hasLeadership).orElse(false);
  }

  private void logLeaderZookeeperClientNotFound(String dc) {
    logger.error(
        "Cannot run inactive topics detection - no zookeeper client for datacenter={}", dc);
  }
}
