package pl.allegro.tech.hermes.management.config.console;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.management.domain.console.ConsoleConfigurationRepository;
import pl.allegro.tech.hermes.management.infrastructure.console.ClasspathFileConsoleConfigurationRepository;
import pl.allegro.tech.hermes.management.infrastructure.console.FrontendRoutesFilter;
import pl.allegro.tech.hermes.management.infrastructure.console.HttpConsoleConfigurationRepository;
import pl.allegro.tech.hermes.management.infrastructure.console.SpringConfigConsoleConfigurationRepository;

@Configuration
@EnableConfigurationProperties({ConsoleConfigProperties.class, ConsoleProperties.class})
public class ConsoleConfiguration {

  @Bean
  FilterRegistrationBean<FrontendRoutesFilter> frontendRoutesFilter() {
    FilterRegistrationBean<FrontendRoutesFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new FrontendRoutesFilter());
    return registrationBean;
  }

  @Bean
  ConsoleConfigurationRepository consoleConfigurationRepository(
      ConsoleConfigProperties properties,
      ObjectMapper objectMapper,
      ConsoleProperties consoleProperties) {
    switch (properties.getType()) {
      case CLASSPATH_RESOURCE:
        return new ClasspathFileConsoleConfigurationRepository(properties);
      case HTTP_RESOURCE:
        return httpConsoleConfigurationRepository(properties);
      case SPRING_CONFIG:
        return new SpringConfigConsoleConfigurationRepository(objectMapper, consoleProperties);
      default:
        throw new IllegalArgumentException("Unsupported console config type");
    }
  }

  private ConsoleConfigurationRepository httpConsoleConfigurationRepository(
      ConsoleConfigProperties properties) {
    var httpClientProperties = properties.getHttpClient();
    var socketConfig =
        SocketConfig.custom()
            .setSoTimeout(
                (int) httpClientProperties.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .build();
    var connectionManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultSocketConfig(socketConfig)
            .build();
    var client = HttpClientBuilder.create().setConnectionManager(connectionManager).build();
    HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory(client);
    requestFactory.setConnectTimeout(properties.getHttpClient().getConnectTimeout());
    RestTemplate restTemplate = new RestTemplate(requestFactory);
    return new HttpConsoleConfigurationRepository(properties, restTemplate);
  }
}
