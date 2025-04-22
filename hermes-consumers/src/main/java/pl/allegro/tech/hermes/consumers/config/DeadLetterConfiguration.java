package pl.allegro.tech.hermes.consumers.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.tracker.consumers.deadletters.DeadLetters;
import pl.allegro.tech.hermes.tracker.consumers.deadletters.DeadRepository;

import java.util.List;

@Configuration

public class DeadLetterConfiguration {

    @Bean(destroyMethod = "close")
    public DeadLetters deadLetters(List<DeadRepository> repositories){
        return new DeadLetters(repositories);
    }

}
