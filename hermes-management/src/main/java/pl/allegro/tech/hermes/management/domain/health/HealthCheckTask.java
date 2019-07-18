package pl.allegro.tech.hermes.management.domain.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class HealthCheckTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckTask.class);

    private final Collection<ZookeeperClient> zookeeperClients;
    private final String healthCheckPath;
    private final ObjectMapper objectMapper;
    private final ModeService modeService;
    private final HermesMetrics metrics;

    HealthCheckTask(Collection<ZookeeperClient> zookeeperClients, String healthCheckPath, ObjectMapper objectMapper,
                    ModeService modeService, HermesMetrics metrics) {
        this.zookeeperClients = zookeeperClients;
        this.healthCheckPath = healthCheckPath;
        this.objectMapper = objectMapper;
        this.modeService = modeService;
        this.metrics = metrics;
    }

    @Override
    public void run() {
        final List<HealthCheckResult> healthCheckResults = zookeeperClients.stream()
                .map(this::doHealthCheck)
                .collect(Collectors.toList());
        updateMode(healthCheckResults);
    }

    private HealthCheckResult doHealthCheck(ZookeeperClient zookeeperClient) {
        final String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        try {
            zookeeperClient.ensureEphemeralNodeExists(healthCheckPath);
            zookeeperClient.getCuratorFramework()
                    .setData()
                    .forPath(healthCheckPath, objectMapper.writeValueAsBytes(timestamp));
            metrics.counter("storage-health-check.successful").inc();
            logger.info("Storage healthy for datacenter {}", zookeeperClient.getDatacenterName());
            return HealthCheckResult.HEALTHY;
        } catch (Exception e) {
            metrics.counter("storage-health-check.failed").inc();
            logger.error("Storage health check failed for datacenter {}", zookeeperClient.getDatacenterName(), e);
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
        HEALTHY, UNHEALTHY
    }
}
