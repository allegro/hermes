package pl.allegro.tech.hermes.management.config;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
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
import pl.allegro.tech.hermes.management.domain.owner.PlaintextOwnerSource;
import pl.allegro.tech.hermes.management.infrastructure.crowd.CachedCrowdClient;
import pl.allegro.tech.hermes.management.infrastructure.crowd.RestCrowdClient;

@Configuration
@EnableConfigurationProperties(CrowdProperties.class)
@ConditionalOnProperty("owner.crowd.enabled")
public class CrowdConfiguration {

    @Bean("managementRequestFactory")
    @ConfigurationProperties(prefix = "management.rest-template")
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory();
    }

    @Bean(name = "crowdRestTemplate")
    public RestTemplate restTemplate(CrowdProperties properties) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(properties.getConnectionTimeoutMillis())
                .setSocketTimeout(properties.getSocketTimeoutMillis())
                .build();

        HttpClient client = HttpClientBuilder.create()
                .setMaxConnTotal(properties.getMaxConnections())
                .setMaxConnPerRoute(properties.getMaxConnectionsPerRoute())
                .setDefaultRequestConfig(requestConfig)
                .build();

        ClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(client);

        return new RestTemplate(clientHttpRequestFactory);
    }

    @Bean
    @Order(PlaintextOwnerSource.ORDER + 1)
    public CrowdOwnerSource crowdOwnerSource(CrowdProperties crowdProperties, @Qualifier("crowdRestTemplate") RestTemplate restTemplate) {
        return new CrowdOwnerSource(new CachedCrowdClient(new RestCrowdClient(restTemplate, crowdProperties), crowdProperties));
    }

}
