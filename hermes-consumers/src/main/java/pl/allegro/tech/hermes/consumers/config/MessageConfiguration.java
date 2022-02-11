package pl.allegro.tech.hermes.consumers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;

@Configuration
public class MessageConfiguration {

    @Bean
    public UndeliveredMessageLogPersister undeliveredMessageLogPersister(UndeliveredMessageLog undeliveredMessageLog,
                                                                         ConfigFactory configFactory) {
        return new UndeliveredMessageLogPersister(undeliveredMessageLog, configFactory);
    }
}
