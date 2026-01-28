package pl.allegro.tech.hermes.consumers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.test.helper.endpoint.MultiUrlEndpointAddressResolver;

@Configuration
public class EndpointAddressResolverConfiguration {

  @Bean
  @Primary
  @Profile("integration")
  public EndpointAddressResolver testMultiUrlEndpointAddressResolver() {
    return new MultiUrlEndpointAddressResolver();
  }
}
