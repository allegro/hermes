package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.management.domain.console.ConsoleConfigurationRepository;
import pl.allegro.tech.hermes.management.infrastructure.console.ClasspathConsoleConfigurationRepository;
import pl.allegro.tech.hermes.management.infrastructure.console.HttpConsoleConfigurationRepository;

@Configuration
@EnableConfigurationProperties(ConsoleProperties.class)
public class ConsoleConfiguration {

    private ConsoleProperties properties;

    public ConsoleConfiguration(ConsoleProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(name = "console.configuration-type", havingValue = "http_resource")
    public ConsoleConfigurationRepository httpConsoleConfigurationRepository() {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getHttpClient().getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.getHttpClient().getReadTimeout().toMillis());
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        return new HttpConsoleConfigurationRepository(properties, restTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "console.configuration-type", havingValue = "classpath_resource", matchIfMissing = true)
    public ConsoleConfigurationRepository classpathConsoleConfigurationRepository() {
        return new ClasspathConsoleConfigurationRepository(properties);
    }
}
