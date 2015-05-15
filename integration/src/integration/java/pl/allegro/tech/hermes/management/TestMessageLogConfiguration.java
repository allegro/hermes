package pl.allegro.tech.hermes.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.integration.env.FongoFactory;

@Configuration
public class TestMessageLogConfiguration {

    @Autowired
    ObjectMapper objectMapper;
    
    @Bean
    DB messageLogDatabase() {
        return FongoFactory.getInstance().getDB("hermesMessages");
    }
    
}
