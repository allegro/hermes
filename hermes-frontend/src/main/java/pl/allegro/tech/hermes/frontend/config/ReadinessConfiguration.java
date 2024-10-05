package pl.allegro.tech.hermes.frontend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Named;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.frontend.producer.BrokerTopicAvailabilityChecker;
import pl.allegro.tech.hermes.frontend.readiness.AdminReadinessService;
import pl.allegro.tech.hermes.frontend.readiness.DefaultReadinessChecker;
import pl.allegro.tech.hermes.frontend.readiness.HealthCheckService;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

@Configuration
@EnableConfigurationProperties({ReadinessCheckProperties.class})
public class ReadinessConfiguration {

  @Bean
  public DefaultReadinessChecker readinessChecker(
      ReadinessCheckProperties readinessCheckProperties,
      @Named("localDatacenterBrokerProducer")
          BrokerTopicAvailabilityChecker brokerTopicAvailabilityChecker,
      AdminReadinessService adminReadinessService) {
    return new DefaultReadinessChecker(
        brokerTopicAvailabilityChecker,
        adminReadinessService,
        readinessCheckProperties.isEnabled(),
        readinessCheckProperties.isKafkaCheckEnabled(),
        readinessCheckProperties.getInterval());
  }

  @Bean(initMethod = "start", destroyMethod = "stop")
  public AdminReadinessService adminReadinessService(
      ObjectMapper mapper,
      CuratorFramework zookeeper,
      ZookeeperPaths paths,
      DatacenterNameProvider datacenterNameProvider) {
    String localDatacenterName = datacenterNameProvider.getDatacenterName();
    return new AdminReadinessService(mapper, zookeeper, paths, localDatacenterName);
  }

  @Bean(initMethod = "startup")
  public HealthCheckService healthCheckService() {
    return new HealthCheckService();
  }
}
