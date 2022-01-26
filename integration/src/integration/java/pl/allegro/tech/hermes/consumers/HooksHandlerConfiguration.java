package pl.allegro.tech.hermes.consumers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import pl.allegro.tech.hermes.consumers.hooks.SpringHooksHandler;

public class HooksHandlerConfiguration {

    @Bean
    @Primary
    SpringHooksHandler springHooksHandler() {
        SpringHooksHandler hooksHandler = new SpringHooksHandler();
        hooksHandler.disableGlobalShutdownHook();
        return hooksHandler;
    }
}
