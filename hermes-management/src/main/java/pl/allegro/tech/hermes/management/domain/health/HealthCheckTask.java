package pl.allegro.tech.hermes.management.domain.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class HealthCheckTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckTask.class);

    private final Collection<ZookeeperClient> zookeeperClients;
    private final String healthCheckPath;
    private final ObjectMapper objectMapper;
    private final ModeService modeService;

    HealthCheckTask(Collection<ZookeeperClient> zookeeperClients, String healthCheckPath, ObjectMapper objectMapper, ModeService modeService) {
        this.zookeeperClients = zookeeperClients;
        this.healthCheckPath = healthCheckPath;
        this.objectMapper = objectMapper;
        this.modeService = modeService;
    }

    @Override
    public void run() {
        final List<HealthCheckResult> healthChecks = zookeeperClients.stream()
                .map(this::doHealthCheck)
                .collect(Collectors.toList());
        if (!modeService.isReadOnlyEnabled() && healthChecks.contains(HealthCheckResult.UNHEALTHY)) {
            modeService.setMode(ModeService.ManagementMode.READ_ONLY);
        } else if (modeService.isReadOnlyEnabled() && !healthChecks.contains(HealthCheckResult.UNHEALTHY)) {
            modeService.setMode(ModeService.ManagementMode.READ_WRITE);
        }
    }

    private HealthCheckResult doHealthCheck(ZookeeperClient zookeeperClient) {
        try {
            final String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            zookeeperClient.getCuratorFramework()
                    .setData()
                    .forPath(healthCheckPath, objectMapper.writeValueAsBytes(timestamp));
            LOGGER.info("ZooKeeper {} healthy.", zookeeperClient.getDcName());
            return HealthCheckResult.HEALTHY;
        } catch (Exception e) {
            LOGGER.error("Cannot connect to ZooKeeper {}.", zookeeperClient.getDcName(), e);
            return HealthCheckResult.UNHEALTHY;
        }
    }

    enum HealthCheckResult {
        HEALTHY, UNHEALTHY
    }
}
