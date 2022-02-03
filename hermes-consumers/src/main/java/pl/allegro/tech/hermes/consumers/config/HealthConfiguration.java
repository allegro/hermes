package pl.allegro.tech.hermes.consumers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.consumers.health.ConsumerHttpServer;
import pl.allegro.tech.hermes.consumers.health.ConsumerMonitor;

import java.io.IOException;

@Configuration
public class HealthConfiguration {

    @Bean
    public ConsumerHttpServer consumerHttpServer(ConfigFactory configFactory,
                                                 ConsumerMonitor monitor,
                                                 ObjectMapper mapper) throws IOException {
        return new ConsumerHttpServer(configFactory, monitor, mapper);
    }

    @Bean
    public ConsumerMonitor consumerMonitor() {
        return new ConsumerMonitor();
    }
}
