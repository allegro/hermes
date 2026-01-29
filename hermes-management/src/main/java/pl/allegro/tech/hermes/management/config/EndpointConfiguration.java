package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.api.auth.AllowAllSecurityProvider;
import pl.allegro.tech.hermes.management.api.auth.SecurityProvider;

@Configuration
@EnableConfigurationProperties(JerseyProperties.class)
public class EndpointConfiguration {

  private final JerseyProperties jerseyProperties;

  public EndpointConfiguration(JerseyProperties jerseyProperties) {
    this.jerseyProperties = jerseyProperties;
  }

  @Bean
  JerseyResourceConfig resourceConfig() {
    return new JerseyResourceConfig(jerseyProperties);
  }

  @Bean
  @ConditionalOnMissingBean(SecurityProvider.class)
  SecurityProvider authorization() {
    return new AllowAllSecurityProvider();
  }
}
