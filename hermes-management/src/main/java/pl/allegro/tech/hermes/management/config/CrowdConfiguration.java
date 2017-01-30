package pl.allegro.tech.hermes.management.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.management.domain.owner.CrowdOwnerSource;
import pl.allegro.tech.hermes.management.infrastructure.crowd.CachedCrowdClient;
import pl.allegro.tech.hermes.management.infrastructure.crowd.RestCrowdClient;

@Configuration
@EnableConfigurationProperties(CrowdProperties.class)
@ConditionalOnProperty("crowd.enabled")
public class CrowdConfiguration {

    @Bean("managementRequestFactory")
    @ConfigurationProperties(prefix = "management.restTemplate")
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory();
    }

    @Bean
    public RestTemplate restTemplate(@Qualifier("managementRequestFactory") ClientHttpRequestFactory clientHttpRequestFactory) {
        return new RestTemplate(clientHttpRequestFactory);
    }

    @Bean
    @Order(1)
    public CrowdOwnerSource crowdOwnerSource(CrowdProperties crowdProperties, RestTemplate restTemplate) {
        return new CrowdOwnerSource(new CachedCrowdClient(new RestCrowdClient(restTemplate, crowdProperties), crowdProperties));
    }

}
