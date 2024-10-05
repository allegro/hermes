package pl.allegro.tech.hermes.management.domain.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient;

class HealthCheckTask implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(HealthCheckTask.class);

  private final Collection<ZookeeperClient> zookeeperClients;
  private final String healthCheckPath;
  private final ObjectMapper objectMapper;
  private final ModeService modeService;
  private final MeterRegistry meterRegistry;

  HealthCheckTask(
      Collection<ZookeeperClient> zookeeperClients,
      String healthCheckPath,
      ObjectMapper objectMapper,
      ModeService modeService,
      MeterRegistry meterRegistry) {
    this.zookeeperClients = zookeeperClients;
    this.healthCheckPath = healthCheckPath;
    this.objectMapper = objectMapper;
    this.modeService = modeService;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void run() {
    final List<HealthCheckResult> healthCheckResults =
        zookeeperClients.stream().map(this::doHealthCheck).collect(Collectors.toList());
    updateMode(healthCheckResults);
  }

  private HealthCheckResult doHealthCheck(ZookeeperClient zookeeperClient) {
    final String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    try {
      zookeeperClient.ensureEphemeralNodeExists(healthCheckPath);
      zookeeperClient
          .getCuratorFramework()
          .setData()
          .forPath(healthCheckPath, objectMapper.writeValueAsBytes(timestamp));
      meterRegistry.counter("storage-health-check.successful").increment();
      return HealthCheckResult.HEALTHY;
    } catch (Exception e) {
      meterRegistry.counter("storage-health-check.failed").increment();
      logger.error(
          "Storage health check failed for datacenter {}", zookeeperClient.getDatacenterName(), e);
      return HealthCheckResult.UNHEALTHY;
    }
  }

  private void updateMode(List<HealthCheckResult> healthCheckResults) {
    if (healthCheckResults.contains(HealthCheckResult.UNHEALTHY)) {
      modeService.setMode(ModeService.ManagementMode.READ_ONLY);
    } else {
      modeService.setMode(ModeService.ManagementMode.READ_WRITE);
    }
  }

  private enum HealthCheckResult {
    HEALTHY,
    UNHEALTHY
  }
}
