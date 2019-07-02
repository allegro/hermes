package pl.allegro.tech.hermes.management.domain.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class HealthChecker {

    private final ZookeeperClientManager zookeeperClientManager;
    private final String healthCheckPath;
    private final HealthCheckTask healthCheckTask;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @Inject
    public HealthChecker(ZookeeperClientManager zookeeperClientManager, NodeDataProvider nodeDataProvider,
                         ObjectMapper objectMapper, ModeService modeService) {
        this.zookeeperClientManager = zookeeperClientManager;
        this.healthCheckPath = String.format("/hermes/management/health/%s:%s", nodeDataProvider.getHostname(), nodeDataProvider.getServerPort());
        this.healthCheckTask = new HealthCheckTask(zookeeperClientManager.getClients(), this.healthCheckPath, objectMapper, modeService);
    }

    @PostConstruct
    public void scheduleHealthCheck() {
        zookeeperClientManager.getClients()
                .forEach(this::setupHealthPath);
        executorService.scheduleAtFixedRate(healthCheckTask, 0, 2, TimeUnit.SECONDS);
    }

    // TODO: Skorzystac z ensurePathExists
    private void setupHealthPath(ZookeeperClient zookeeperClient) {
        try {
            final boolean healthPathExists = zookeeperClient.getCuratorFramework()
                    .checkExists()
                    .forPath(healthCheckPath) != null;

            if (!healthPathExists) {
                zookeeperClient.getCuratorFramework()
                        .create()
                        .creatingParentsIfNeeded()
                        .forPath(healthCheckPath);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
