package pl.allegro.tech.hermes.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.config.storage.StorageClustersProperties;
import pl.allegro.tech.hermes.management.config.storage.StorageProperties;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.readiness.DatacenterReadinessRepository;
import pl.allegro.tech.hermes.management.domain.readiness.ReadinessService;

import java.util.List;

@Configuration
public class ReadinessConfiguration {

    @Bean
    ReadinessService readinessService(MultiDatacenterRepositoryCommandExecutor commandExecutor,
                                      DatacenterReadinessRepository readinessRepository,
                                      StorageClustersProperties storageClustersProperties) {
        List<String> datacenters = storageClustersProperties.getClusters().stream()
                .map(StorageProperties::getDatacenter)
                .toList();
        return new ReadinessService(commandExecutor, readinessRepository, datacenters);
    }
}
