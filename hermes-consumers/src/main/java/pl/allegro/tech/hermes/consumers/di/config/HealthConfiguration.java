package pl.allegro.tech.hermes.consumers.di.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.health.ConsumerHttpServer;
import pl.allegro.tech.hermes.consumers.health.ConsumerMonitor;

import java.io.IOException;

import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG;

@Configuration
public class HealthConfiguration {

    @Bean
    public ConsumerHttpServer consumerHttpServer(ConfigFactory configFactory,
                                                 ConsumerMonitor monitor,
                                                 ObjectMapper mapper) throws IOException {
//        Configs.CONSUMER_HEALTH_CHECK_PORT 7454
//        Configs.METRICS_GRAPHITE_REPORTER false
//        Configs.METRICS_ZOOKEEPER_REPORTER false
//        overrideProperty(KAFKA_CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG, 50);
        return new ConsumerHttpServer(configFactory, monitor, mapper);
    }

    @Bean
    public ConsumerMonitor consumerMonitor() {
        return new ConsumerMonitor();
    }
}
