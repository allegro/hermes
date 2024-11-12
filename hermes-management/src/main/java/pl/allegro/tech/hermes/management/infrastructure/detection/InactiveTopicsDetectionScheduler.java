package pl.allegro.tech.hermes.management.infrastructure.detection;

import jakarta.annotation.PostConstruct;
import java.util.Optional;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.config.detection.InactiveTopicsDetectionProperties;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsDetectionJob;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager;

@ConditionalOnProperty(value = "detection.inactive-topics.enabled", havingValue = "true")
@Component
@EnableConfigurationProperties(InactiveTopicsDetectionProperties.class)
public class InactiveTopicsDetectionScheduler {
  private final InactiveTopicsDetectionJob job;
  private final String leaderElectionDc;
  private final Optional<LeaderLatch> leaderLatch;

  private static final Logger logger =
      LoggerFactory.getLogger(InactiveTopicsDetectionScheduler.class);

  public InactiveTopicsDetectionScheduler(
      InactiveTopicsDetectionJob job,
      ZookeeperClientManager zookeeperClientManager,
      InactiveTopicsDetectionProperties inactiveTopicsDetectionProperties,
      ZookeeperPaths zookeeperPaths) {
    this.leaderElectionDc = inactiveTopicsDetectionProperties.leaderElectionZookeeperDc();
    String leaderPath = zookeeperPaths.inactiveTopicsLeaderPath();
    Optional<CuratorFramework> leaderCuratorFramework =
        zookeeperClientManager.getClients().stream()
            .filter(it -> it.getDatacenterName().equals(leaderElectionDc))
            .findFirst()
            .map(ZookeeperClient::getCuratorFramework);
    this.leaderLatch = leaderCuratorFramework.map(it -> new LeaderLatch(it, leaderPath));
    if (leaderLatch.isEmpty()) {
      logLeaderZookeeperClientNotFound(leaderElectionDc);
    }
    this.job = job;
  }

  @PostConstruct
  public void startListeningForLeadership() {
    leaderLatch.ifPresent(
        it -> {
          try {
            it.start();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Scheduled(cron = "${detection.inactive-topics.cron}")
  public void run() {
    if (leaderLatch.isPresent()) {
      if (leaderLatch.get().hasLeadership()) {
        job.detectAndNotify();
      }
    } else {
      logLeaderZookeeperClientNotFound(leaderElectionDc);
    }
  }

  private void logLeaderZookeeperClientNotFound(String dc) {
    logger.error("Cannot run inactive topics detection - no zookeeper client for datacenter={}", dc);
  }
}
