package pl.allegro.tech.hermes.consumers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.io.IOException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.consumers.health.ConsumerMonitor;
import pl.allegro.tech.hermes.consumers.server.ConsumerHttpServer;

@Configuration
@EnableConfigurationProperties(CommonConsumerProperties.class)
public class ServerConfiguration {

  @Bean(initMethod = "start", destroyMethod = "stop")
  public ConsumerHttpServer consumerHttpServer(
      CommonConsumerProperties commonConsumerProperties,
      ConsumerMonitor monitor,
      ObjectMapper mapper,
      PrometheusMeterRegistry meterRegistry)
      throws IOException {
    return new ConsumerHttpServer(
        commonConsumerProperties.getHealthCheckPort(), monitor, mapper, meterRegistry);
  }

  @Bean
  public ConsumerMonitor consumerMonitor() {
    return new ConsumerMonitor();
  }
}
