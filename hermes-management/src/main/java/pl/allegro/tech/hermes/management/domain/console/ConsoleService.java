package pl.allegro.tech.hermes.management.domain.console;

public class ConsoleService {

  private final ConsoleConfigurationRepository repository;

  public ConsoleService(ConsoleConfigurationRepository repository) {
    this.repository = repository;
  }

  public String getConfigurationJson() {
    return repository.getConfiguration();
  }
}
