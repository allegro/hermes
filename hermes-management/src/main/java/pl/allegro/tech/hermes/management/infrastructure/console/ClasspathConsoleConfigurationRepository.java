package pl.allegro.tech.hermes.management.infrastructure.console;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import pl.allegro.tech.hermes.management.config.ConsoleProperties;
import pl.allegro.tech.hermes.management.domain.console.ConsoleConfigurationRepository;

import java.nio.charset.StandardCharsets;

public class ClasspathConsoleConfigurationRepository implements ConsoleConfigurationRepository {

    private String configuration;

    public ClasspathConsoleConfigurationRepository(ConsoleProperties properties) {
        configuration = loadConfiguration(properties.getConfigurationLocation());
    }

    @Override
    public String find() {
        return configuration;
    }

    private String loadConfiguration(String location) {
        try {
            ClassPathResource resource = new ClassPathResource(location);
            byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error reading Hermes Console configuration from " + location, e);
        }
    }
}
