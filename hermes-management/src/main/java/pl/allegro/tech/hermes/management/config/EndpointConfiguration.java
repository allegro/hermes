package pl.allegro.tech.hermes.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.api.auth.AllowAllSecurityContextProvider;
import pl.allegro.tech.hermes.management.api.auth.SecurityContextProvider;
import pl.allegro.tech.hermes.management.infrastructure.query.parser.QueryParser;
import pl.allegro.tech.hermes.management.infrastructure.query.parser.json.JsonQueryParser;

import java.util.Optional;

@Configuration
@EnableConfigurationProperties(JerseyProperties.class)
public class EndpointConfiguration {

    @Autowired
    private JerseyProperties jerseyProperties;

    @Bean
    @ConditionalOnMissingBean(QueryParser.class)
    public QueryParser queryParser(Optional<ObjectMapper> objectMapper) {
        return new JsonQueryParser(objectMapper.orElse(new ObjectMapper()));
    }

    @Bean
    JerseyResourceConfig resourceConfig() {
        return new JerseyResourceConfig(jerseyProperties.getPackagesToScan());
    }

    @Bean
    @ConditionalOnMissingBean(SecurityContextProvider.class)
    SecurityContextProvider authorization() {
        return new AllowAllSecurityContextProvider();
    }
}
