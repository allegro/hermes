package pl.allegro.tech.hermes.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.integration.env.FongoFactory;
import pl.allegro.tech.hermes.message.tracker.management.LogRepository;
import pl.allegro.tech.hermes.message.tracker.mongo.management.MongoLogRepository;

@Configuration
public class TestMessageLogConfiguration {

    @Autowired
    ObjectMapper objectMapper;
    
    @Bean
    DB messageLogDatabase() {
        return FongoFactory.hermesDB();
    }

    @Bean
    LogRepository logRepository(DB messageLogDatabase) {
        return new MongoLogRepository(messageLogDatabase);
    }
    
}
