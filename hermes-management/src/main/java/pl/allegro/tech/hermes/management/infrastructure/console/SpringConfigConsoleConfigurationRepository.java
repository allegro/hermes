package pl.allegro.tech.hermes.management.infrastructure.console;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.management.config.console.ConsoleProperties;
import pl.allegro.tech.hermes.management.domain.console.ConsoleConfigurationRepository;

public class SpringConfigConsoleConfigurationRepository implements ConsoleConfigurationRepository {

  private final String jsonConfig;

  public SpringConfigConsoleConfigurationRepository(
      ObjectMapper objectMapper, ConsoleProperties consoleProperties) {
    try {
      jsonConfig = objectMapper.writeValueAsString(consoleProperties);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(
          "Error during spring console config conversion to JSON", e);
    }
  }

  @Override
  public String getConfiguration() {
    return jsonConfig;
  }
}
