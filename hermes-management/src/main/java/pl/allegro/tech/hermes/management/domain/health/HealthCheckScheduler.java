package pl.allegro.tech.hermes.management.domain.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "management.health.enabled", havingValue = "true")
public class HealthCheckScheduler {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckScheduler.class);

    private final HealthCheckTask healthCheckTask;
    private final Long period;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("storage-health-check-scheduler-%d").build()
    );

    public HealthCheckScheduler(ZookeeperClientManager zookeeperClientManager,
                                ZookeeperPaths zookeeperPaths,
                                NodeDataProvider nodeDataProvider,
                                ObjectMapper objectMapper,
                                ModeService modeService,
                                MeterRegistry meterRegistry,
                                @Value("${management.health.periodSeconds}") Long periodSeconds) {
        String healthCheckPath = zookeeperPaths.nodeHealthPathForManagementHost(nodeDataProvider.getHostname(), nodeDataProvider.getServerPort());
        this.period = periodSeconds;
        this.healthCheckTask = new HealthCheckTask(zookeeperClientManager.getClients(), healthCheckPath, objectMapper, modeService, meterRegistry);
    }

    @PostConstruct
    public void scheduleHealthCheck() {
        logger.info("Starting the storage health check scheduler");
        executorService.scheduleAtFixedRate(healthCheckTask, 0, period, TimeUnit.SECONDS);
    }
}
