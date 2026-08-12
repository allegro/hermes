package pl.allegro.tech.hermes.management.config.console;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.config.GroupProperties;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.console.ConsoleConfigurationRepository;
import pl.allegro.tech.hermes.management.domain.console.ConsoleService;
import pl.allegro.tech.hermes.management.infrastructure.console.FrontendRoutesFilter;
import pl.allegro.tech.hermes.management.infrastructure.console.SpringConfigConsoleConfigurationRepository;

@Configuration
@EnableConfigurationProperties(ConsoleProperties.class)
public class ConsoleConfiguration {
  private static final Logger logger = LoggerFactory.getLogger(ConsoleConfiguration.class);

  @Bean
  FilterRegistrationBean<FrontendRoutesFilter> frontendRoutesFilter() {
    FilterRegistrationBean<FrontendRoutesFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new FrontendRoutesFilter());
    return registrationBean;
  }

  @Bean
  ConsoleConfigurationRepository consoleConfigurationRepository(
      ObjectMapper objectMapper,
      ConsoleProperties consoleProperties,
      GroupProperties groupProperties,
      TopicProperties topicProperties) {

    if (consoleProperties.getTopic().getSchemaRegistryUrl() == null
        || consoleProperties.getTopic().getSchemaRegistryUrl().isBlank()) {
      logger.warn(
          "Console topic schemaRegistryUrl is not configured. Schema versions will not link to Schema Registry.");
    }

    // Override group settings from GroupProperties (source of truth)
    // Note: console.group.nonAdminCreationEnabled is IGNORED if configured in application.yaml
    // See JavaDoc on ConsoleProperties.GroupView for details
    consoleProperties
        .getGroup()
        .setNonAdminCreationEnabled(groupProperties.isNonAdminCreationEnabled());

    // Override content types from TopicProperties (source of truth)
    // Note: console.topic.contentTypes is IGNORED if configured in application.yaml
    // See JavaDoc on ConsoleProperties.TopicView for details
    var contentTypes =
        topicProperties.getAllowedContentTypes().stream()
            .map(
                contentType ->
                    new ConsoleProperties.TopicContentType(contentType.name(), contentType.name()))
            .collect(Collectors.toList());
    consoleProperties.getTopic().setContentTypes(contentTypes);

    return new SpringConfigConsoleConfigurationRepository(objectMapper, consoleProperties);
  }

  @Bean
  public ConsoleService consoleService(ConsoleConfigurationRepository repository) {
    return new ConsoleService(repository);
  }
}
