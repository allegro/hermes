package pl.allegro.tech.hermes.management.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.api.constraints.EndpointAddressValidator;

import javax.annotation.PostConstruct;

@Configuration
@EnableConfigurationProperties(SubscriptionProperties.class)
public class EndpointAddressConfiguration {

    @Autowired
    SubscriptionProperties subscriptionProperties;

    @PostConstruct
    public void setUp() {
        subscriptionProperties.getAdditionalEndpointProtocols().forEach(EndpointAddressValidator::addProtocol);
    }

}
