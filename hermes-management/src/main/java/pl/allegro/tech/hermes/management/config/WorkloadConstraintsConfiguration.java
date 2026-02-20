package pl.allegro.tech.hermes.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.workload.constraints.WorkloadConstraintsService;

@Configuration
public class WorkloadConstraintsConfiguration {

  @Bean
  public WorkloadConstraintsService workloadConstraintsService(
      WorkloadConstraintsRepository workloadConstraintsRepository,
      MultiDatacenterRepositoryCommandExecutor commandExecutor) {
    return new WorkloadConstraintsService(workloadConstraintsRepository, commandExecutor);
  }
}
