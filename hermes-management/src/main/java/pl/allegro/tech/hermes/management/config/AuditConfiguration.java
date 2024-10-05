package pl.allegro.tech.hermes.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.metamodel.clazz.EntityDefinitionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.infrastructure.audit.CompositeAuditor;
import pl.allegro.tech.hermes.management.infrastructure.audit.EventAuditor;
import pl.allegro.tech.hermes.management.infrastructure.audit.LoggingAuditor;

@Configuration
@EnableConfigurationProperties({AuditProperties.class})
public class AuditConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(AuditConfiguration.class);

  @Bean(name = "eventAuditorRestTemplate")
  @ConditionalOnMissingBean(name = "eventAuditorRestTemplate")
  public RestTemplate eventAuditorRestTemplate() {
    logger.info("Creating eventAuditorRestTemplate bean");
    return new RestTemplateBuilder().build();
  }

  @Bean
  @ConditionalOnProperty(prefix = "audit", value = "isLoggingAuditEnabled", havingValue = "true")
  public LoggingAuditor loggingAuditor(ObjectMapper objectMapper) {
    return new LoggingAuditor(javers(), objectMapper);
  }

  @Bean
  @ConditionalOnProperty(prefix = "audit", value = "isEventAuditEnabled", havingValue = "true")
  public EventAuditor eventAuditor(
      AuditProperties auditProperties,
      @Qualifier("eventAuditorRestTemplate") RestTemplate eventAuditorRestTemplate,
      ObjectMapper objectMapper) {
    return new EventAuditor(
        javers(), eventAuditorRestTemplate, auditProperties.getEventUrl(), objectMapper);
  }

  @Bean
  @Primary
  public CompositeAuditor compositeAuditor(Collection<Auditor> auditors) {
    return new CompositeAuditor(auditors);
  }

  private Javers javers() {
    return JaversBuilder.javers()
        .withPrettyPrint(false)
        .registerEntity(
            EntityDefinitionBuilder.entityDefinition(Group.class)
                .withIdPropertyName("groupName")
                .build())
        .registerEntity(
            EntityDefinitionBuilder.entityDefinition(Topic.class)
                .withIdPropertyName("name")
                .build())
        .registerEntity(
            EntityDefinitionBuilder.entityDefinition(Subscription.class)
                .withIdPropertyName("name")
                .build())
        .registerEntity(
            EntityDefinitionBuilder.entityDefinition(OAuthProvider.class)
                .withIdPropertyName("name")
                .build())
        .build();
  }
}
