package pl.allegro.tech.hermes.management;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import pl.allegro.tech.hermes.management.api.auth.SecurityProvider;

@Configuration
public class TestSecurityProviderConfiguration {

  @Bean
  @Primary
  @Profile("integration")
  SecurityProvider testAuthorization() {
    return new TestSecurityProvider();
  }
}
