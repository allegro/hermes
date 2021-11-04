package pl.allegro.tech.hermes.management;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.api.auth.SecurityProvider;

@Configuration
public class TestSecurityProviderConfiguration {

    @Bean
    SecurityProvider authorization() {
        return new TestSecurityProvider();
    }

}
