package pl.allegro.tech.hermes.management.config;

import com.mongodb.DB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDbFactory;

@Configuration
@Profile(value = "!integration")
public class MessageLogConfiguration {

    @Autowired
    MongoDbFactory dbFactory;

    @Bean
    DB messageLogDatabase() {
        return dbFactory.getDb();
    }
}
