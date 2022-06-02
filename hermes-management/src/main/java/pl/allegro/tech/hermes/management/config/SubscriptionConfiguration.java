package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.domain.subscription.validator.EndpointAddressFormatValidator;
import pl.allegro.tech.hermes.management.domain.subscription.validator.EndpointAddressValidator;
import pl.allegro.tech.hermes.management.domain.subscription.validator.EndpointOwnershipValidator;
import pl.allegro.tech.hermes.management.domain.subscription.validator.NoOpEndpointOwnershipValidator;

@Configuration
@EnableConfigurationProperties(SubscriptionProperties.class)
public class SubscriptionConfiguration {

    @Bean
    public EndpointOwnershipValidator defaultEndpointOwnershipValidator() {
        return new NoOpEndpointOwnershipValidator();
    }

    @Bean
    public EndpointAddressValidator endpointAddressFormatValidator(SubscriptionProperties subscriptionProperties) {
        return new EndpointAddressFormatValidator(subscriptionProperties.getAdditionalEndpointProtocols());
    }
}
