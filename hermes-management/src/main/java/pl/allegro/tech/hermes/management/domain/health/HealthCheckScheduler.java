package pl.allegro.tech.hermes.management.domain.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class HealthCheckScheduler {

    private final ZookeeperClientManager zookeeperClientManager;
    private final String healthCheckPath;
    private final HealthCheckTask healthCheckTask;
    private final Long period;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("health-check-scheduler-%d").build()
    );

    @Inject
    public HealthCheckScheduler(ZookeeperClientManager zookeeperClientManager,
                                ZookeeperPaths zookeeperPaths,
                                NodeDataProvider nodeDataProvider,
                                ObjectMapper objectMapper,
                                ModeService modeService,
                                @Value("${management.health.periodSeconds}") Long periodSeconds) {
        this.zookeeperClientManager = zookeeperClientManager;
        this.healthCheckPath = zookeeperPaths.nodeHealthPathForManagementHost(nodeDataProvider.getHostname(), nodeDataProvider.getServerPort());
        this.period = periodSeconds;
        this.healthCheckTask = new HealthCheckTask(zookeeperClientManager.getClients(), this.healthCheckPath, objectMapper, modeService);
    }

    @PostConstruct
    public void scheduleHealthCheck() {
        zookeeperClientManager.getClients().forEach(client -> client.ensurePathExists(healthCheckPath));
        executorService.scheduleAtFixedRate(healthCheckTask, 0, period, TimeUnit.SECONDS);
    }
}
