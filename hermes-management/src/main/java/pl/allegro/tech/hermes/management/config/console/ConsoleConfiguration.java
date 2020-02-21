package pl.allegro.tech.hermes.management.config.console;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.management.domain.console.ConsoleConfigurationRepository;
import pl.allegro.tech.hermes.management.infrastructure.console.ClasspathFileConsoleConfigurationRepository;
import pl.allegro.tech.hermes.management.infrastructure.console.HttpConsoleConfigurationRepository;
import pl.allegro.tech.hermes.management.infrastructure.console.SpringConfigConsoleConfigurationRepository;

@Configuration
@EnableConfigurationProperties({ConsoleConfigProperties.class, ConsoleProperties.class})
public class ConsoleConfiguration {

    @Bean
    ConsoleConfigurationRepository consoleConfigurationRepository(
            ConsoleConfigProperties properties, ObjectMapper objectMapper, ConsoleProperties consoleProperties) {
        switch (properties.getType()) {
            case CLASSPATH_RESOURCE:
                return new ClasspathFileConsoleConfigurationRepository(properties);
            case HTTP_RESOURCE:
                return httpConsoleConfigurationRepository(properties);
            case SPRING_CONFIG:
                return new SpringConfigConsoleConfigurationRepository(objectMapper, consoleProperties);
            default:
                throw new IllegalArgumentException("Unsupported console config type");
        }
    }

    private ConsoleConfigurationRepository httpConsoleConfigurationRepository(ConsoleConfigProperties properties) {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getHttpClient().getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.getHttpClient().getReadTimeout().toMillis());
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        return new HttpConsoleConfigurationRepository(properties, restTemplate);
    }

}
