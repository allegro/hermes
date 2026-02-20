package pl.allegro.tech.hermes.management.config;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSource;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSources;
import pl.allegro.tech.hermes.management.domain.owner.PlaintextOwnerSource;
import pl.allegro.tech.hermes.management.domain.owner.validator.OwnerIdValidator;

@Configuration
public class OwnerConfiguration {

  @Bean
  @Order(PlaintextOwnerSource.ORDER)
  public PlaintextOwnerSource plaintextOwnerSource() {
    return new PlaintextOwnerSource();
  }

  @Bean
  @ConditionalOnMissingBean
  public OwnerSources ownerSources(List<OwnerSource> ownerSources) {
    return new OwnerSources(ownerSources);
  }

  @Bean
  public OwnerIdValidator ownerIdValidator(OwnerSources ownerSources) {
    return new OwnerIdValidator(ownerSources);
  }
}
