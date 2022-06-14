package pl.allegro.tech.hermes.consumers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.consumers.health.ConsumerHttpServer;
import pl.allegro.tech.hermes.consumers.health.ConsumerMonitor;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(CommonConsumerProperties.class)
public class HealthConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public ConsumerHttpServer consumerHttpServer(CommonConsumerProperties commonConsumerProperties,
                                                 ConsumerMonitor monitor,
                                                 ObjectMapper mapper) throws IOException {
        return new ConsumerHttpServer(commonConsumerProperties.getHealthCheckPort(), monitor, mapper);
    }

    @Bean
    public ConsumerMonitor consumerMonitor() {
        return new ConsumerMonitor();
    }
}
