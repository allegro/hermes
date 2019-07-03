package pl.allegro.tech.hermes.management.domain.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @Inject
    public HealthCheckScheduler(ZookeeperClientManager zookeeperClientManager, NodeDataProvider nodeDataProvider,
                                ObjectMapper objectMapper, ModeService modeService,
                                @Value("${management.health.zk-health-path-prefix}") String healthCheckPathPrefix,
                                @Value("${management.health.period}") Long period) {
        this.zookeeperClientManager = zookeeperClientManager;
        this.healthCheckPath = String.format("%s/%s:%s", healthCheckPathPrefix, nodeDataProvider.getHostname(), nodeDataProvider.getServerPort());
        this.period = period;
        this.healthCheckTask = new HealthCheckTask(zookeeperClientManager.getClients(), this.healthCheckPath, objectMapper, modeService);
    }

    @PostConstruct
    public void scheduleHealthCheck() {
        zookeeperClientManager.getClients().forEach(client -> client.ensurePathExists(healthCheckPath));
        executorService.scheduleAtFixedRate(healthCheckTask, 0, period, TimeUnit.SECONDS);
    }
}
