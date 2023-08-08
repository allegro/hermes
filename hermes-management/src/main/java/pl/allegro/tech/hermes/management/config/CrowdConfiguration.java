package pl.allegro.tech.hermes.management.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
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
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(properties.getMaxConnections())
                .setMaxConnPerRoute(properties.getMaxConnectionsPerRoute())
                .setDefaultConnectionConfig(
                        ConnectionConfig.custom()
                                .setSocketTimeout(Timeout.ofMilliseconds(properties.getSocketTimeoutMillis()))
                                .setConnectTimeout(Timeout.ofMilliseconds(properties.getConnectionTimeoutMillis()))
                                .build())
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(properties.getConnectionTimeoutMillis()))
                .setResponseTimeout(Timeout.ofMilliseconds(properties.getSocketTimeoutMillis()))
                .build();

        HttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
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
