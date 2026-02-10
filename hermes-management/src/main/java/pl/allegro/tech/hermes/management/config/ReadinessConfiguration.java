package pl.allegro.tech.hermes.management.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.config.zookeeper.ZookeeperClustersProperties;
import pl.allegro.tech.hermes.management.config.zookeeper.ZookeeperProperties;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.readiness.DatacenterReadinessRepository;
import pl.allegro.tech.hermes.management.domain.readiness.ReadinessService;

@Configuration
public class ReadinessConfiguration {

  @Bean
  ReadinessService readinessService(
      MultiDatacenterRepositoryCommandExecutor commandExecutor,
      DatacenterReadinessRepository readinessRepository,
      ZookeeperClustersProperties zookeeperClustersProperties) {
    List<String> datacenters =
        zookeeperClustersProperties.getClusters().stream()
            .map(ZookeeperProperties::getDatacenter)
            .toList();
    return new ReadinessService(commandExecutor, readinessRepository, datacenters);
  }
}
