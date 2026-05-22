package pl.allegro.tech.hermes.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.health.HealthCheckScheduler;
import pl.allegro.tech.hermes.management.domain.health.NodeDataProvider;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager;

@Configuration
@EnableConfigurationProperties({HealthProperties.class, ServerPortProperties.class})
public class HealthConfiguration {

  @Bean
  public NodeDataProvider nodeDataProvider(ServerPortProperties serverPortProperties) {
    return new NodeDataProvider(serverPortProperties.getPort());
  }

  @Bean(initMethod = "scheduleHealthCheck")
  public HealthCheckScheduler healthCheckScheduler(
      ZookeeperClientManager zookeeperClientManager,
      ZookeeperPaths zookeeperPaths,
      NodeDataProvider nodeDataProvider,
      ObjectMapper objectMapper,
      ModeService modeService,
      MeterRegistry meterRegistry,
      HealthProperties healthProperties) {
    return new HealthCheckScheduler(
        zookeeperClientManager,
        zookeeperPaths,
        nodeDataProvider,
        objectMapper,
        modeService,
        meterRegistry,
        healthProperties.getPeriod(),
        healthProperties.isEnabled());
  }
}
