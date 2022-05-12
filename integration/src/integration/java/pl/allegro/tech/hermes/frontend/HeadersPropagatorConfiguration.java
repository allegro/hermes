package pl.allegro.tech.hermes.frontend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;
import pl.allegro.tech.hermes.integration.metadata.TraceHeadersPropagator;

@Configuration
public class HeadersPropagatorConfiguration {

    @Bean
    @Primary
    @Profile("integration")
    HeadersPropagator traceHeadersPropagator() {
        return new TraceHeadersPropagator();
    }
}
