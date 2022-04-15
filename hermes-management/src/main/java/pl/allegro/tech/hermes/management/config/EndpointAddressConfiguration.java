package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.domain.endpoint.AdditionalEndpointAddressValidator;
import pl.allegro.tech.hermes.management.domain.endpoint.EndpointAddressValidator;

import java.util.List;

@Configuration
@EnableConfigurationProperties(SubscriptionProperties.class)
public class EndpointAddressConfiguration {

    @Bean
    public EndpointAddressValidator endpointAddressValidator(SubscriptionProperties subscriptionProperties,
                                                             AdditionalEndpointAddressValidator additionalEndpointAddressValidator) {
        List<String> additionalEndpointProtocols = subscriptionProperties.getAdditionalEndpointProtocols();
        return new EndpointAddressValidator(additionalEndpointProtocols, additionalEndpointAddressValidator);
    }

    @Bean
    public AdditionalEndpointAddressValidator defaultAdditionalEndpointAddressValidator() {
        return address -> {};
    }
}
