package pl.allegro.tech.hermes.management.domain.console;

import org.springframework.stereotype.Service;

@Service
public class ConsoleService {

  private ConsoleConfigurationRepository repository;

  public ConsoleService(ConsoleConfigurationRepository repository) {
    this.repository = repository;
  }

  public String getConfigurationJson() {
    return repository.getConfiguration();
  }
}
