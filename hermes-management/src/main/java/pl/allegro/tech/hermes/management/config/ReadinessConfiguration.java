package pl.allegro.tech.hermes.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.readiness.ReadinessService;

@Configuration
public class ReadinessConfiguration {
    @Bean
    ReadinessService readinessService(MultiDatacenterRepositoryCommandExecutor commandExecutor) {
        return new ReadinessService(commandExecutor);
    }
}
