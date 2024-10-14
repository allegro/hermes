package pl.allegro.tech.hermes.management.infrastructure.console;

import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import pl.allegro.tech.hermes.management.config.console.ConsoleConfigProperties;
import pl.allegro.tech.hermes.management.domain.console.ConsoleConfigurationRepository;

public class ClasspathFileConsoleConfigurationRepository implements ConsoleConfigurationRepository {

  private String configuration;

  public ClasspathFileConsoleConfigurationRepository(ConsoleConfigProperties properties) {
    configuration = loadConfiguration(properties.getLocation());
  }

  @Override
  public String getConfiguration() {
    return configuration;
  }

  private String loadConfiguration(String location) {
    try {
      ClassPathResource resource = new ClassPathResource(location);
      byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Error reading Hermes Console configuration from " + location, e);
    }
  }
}
