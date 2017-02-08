package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSource;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSources;

import java.util.List;

@Configuration
public class OwnerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OwnerSources ownerSources(List<OwnerSource> ownerSources) {
        return new OwnerSources(ownerSources);
    }

}
