package pl.allegro.tech.hermes.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;

@Configuration
public class ModeConfiguration {

  @Bean
  public ModeService modeService() {
    return new ModeService();
  }
}
