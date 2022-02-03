package pl.allegro.tech.hermes.consumers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.consumers.hooks.SpringHooksHandler;

@Configuration
public class HooksConfiguration {

    @Bean
    public SpringHooksHandler prodSpringHooksHandler() {
        return new SpringHooksHandler();
    }
}
