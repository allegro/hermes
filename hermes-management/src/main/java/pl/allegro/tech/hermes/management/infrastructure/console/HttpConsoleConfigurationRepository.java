package pl.allegro.tech.hermes.management.infrastructure.console;

import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.management.config.console.ConsoleConfigProperties;
import pl.allegro.tech.hermes.management.domain.console.ConsoleConfigurationRepository;

public class HttpConsoleConfigurationRepository implements ConsoleConfigurationRepository {

  private String configuration;

  public HttpConsoleConfigurationRepository(
      ConsoleConfigProperties properties, RestTemplate restTemplate) {
    configuration = loadConfiguration(properties.getLocation(), restTemplate);
  }

  @Override
  public String getConfiguration() {
    return configuration;
  }

  private String loadConfiguration(String location, RestTemplate restTemplate) {
    try {
      return restTemplate.getForObject(location, String.class);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Error reading Hermes Console configuration from " + location, e);
    }
  }
}
