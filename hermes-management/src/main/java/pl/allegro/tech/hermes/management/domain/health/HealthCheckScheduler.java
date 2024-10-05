package pl.allegro.tech.hermes.management.domain.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager;

@Component
public class HealthCheckScheduler {

  private static final Logger logger = LoggerFactory.getLogger(HealthCheckScheduler.class);

  private final ScheduledExecutorService executorService =
      Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder().setNameFormat("storage-health-check-scheduler-%d").build());
  private final ZookeeperClientManager zookeeperClientManager;
  private final ZookeeperPaths zookeeperPaths;
  private final NodeDataProvider nodeDataProvider;
  private final ObjectMapper objectMapper;
  private final ModeService modeService;
  private final MeterRegistry meterRegistry;
  private final Long periodSeconds;
  private final boolean enabled;

  public HealthCheckScheduler(
      ZookeeperClientManager zookeeperClientManager,
      ZookeeperPaths zookeeperPaths,
      NodeDataProvider nodeDataProvider,
      ObjectMapper objectMapper,
      ModeService modeService,
      MeterRegistry meterRegistry,
      @Value("${management.health.periodSeconds:30}") Long periodSeconds,
      @Value("${management.health.enabled:false}") boolean enabled) {
    this.zookeeperClientManager = zookeeperClientManager;
    this.zookeeperPaths = zookeeperPaths;
    this.nodeDataProvider = nodeDataProvider;
    this.objectMapper = objectMapper;
    this.modeService = modeService;
    this.meterRegistry = meterRegistry;
    this.periodSeconds = periodSeconds;
    this.enabled = enabled;
  }

  @PostConstruct
  public void scheduleHealthCheck() {
    if (enabled) {
      logger.info("Starting the storage health check scheduler");
      String healthCheckPath =
          zookeeperPaths.nodeHealthPathForManagementHost(
              nodeDataProvider.getHostname(), nodeDataProvider.getServerPort());
      HealthCheckTask healthCheckTask =
          new HealthCheckTask(
              zookeeperClientManager.getClients(),
              healthCheckPath,
              objectMapper,
              modeService,
              meterRegistry);
      executorService.scheduleAtFixedRate(healthCheckTask, 0, periodSeconds, TimeUnit.SECONDS);
    } else {
      logger.info("Storage health check is disabled");
      modeService.setMode(ModeService.ManagementMode.READ_WRITE);
    }
  }
}
