package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSource;
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSources;

import java.util.List;

@Configuration
public class MaintainerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MaintainerSources maintainerSources(List<MaintainerSource> maintainerSources) {
        return new MaintainerSources(maintainerSources);
    }

}
