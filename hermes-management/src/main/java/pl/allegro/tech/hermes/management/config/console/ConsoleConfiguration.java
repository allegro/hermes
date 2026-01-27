package pl.allegro.tech.hermes.management.config.console;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.domain.console.ConsoleConfigurationRepository;
import pl.allegro.tech.hermes.management.infrastructure.console.FrontendRoutesFilter;
import pl.allegro.tech.hermes.management.infrastructure.console.SpringConfigConsoleConfigurationRepository;

@Configuration
@EnableConfigurationProperties(ConsoleProperties.class)
public class ConsoleConfiguration {

  @Bean
  FilterRegistrationBean<FrontendRoutesFilter> frontendRoutesFilter() {
    FilterRegistrationBean<FrontendRoutesFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new FrontendRoutesFilter());
    return registrationBean;
  }

  @Bean
  ConsoleConfigurationRepository consoleConfigurationRepository(
      ObjectMapper objectMapper, ConsoleProperties consoleProperties) {
    return new SpringConfigConsoleConfigurationRepository(objectMapper, consoleProperties);
  }
}
