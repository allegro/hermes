package pl.allegro.tech.hermes.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.infrastructure.audit.LoggingAuditor;

@Configuration
public class AuditConfiguration {

    @Bean
    public Auditor auditor(ObjectMapper objectMapper) {
        return new LoggingAuditor(javers(), objectMapper);
    }

    private Javers javers() {
        return JaversBuilder.javers().withPrettyPrint(false).build();
    }
}
