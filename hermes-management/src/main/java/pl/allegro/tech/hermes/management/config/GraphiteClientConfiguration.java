package pl.allegro.tech.hermes.management.config;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.management.infrastructure.graphite.CachingGraphiteClient;
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteClient;
import pl.allegro.tech.hermes.management.infrastructure.graphite.RestTemplateGraphiteClient;
import pl.allegro.tech.hermes.management.stub.MetricsPaths;

import java.net.URI;

import static com.google.common.base.Ticker.systemTicker;

@Configuration
@EnableConfigurationProperties({MetricsProperties.class, GraphiteClientProperties.class})
public class GraphiteClientConfiguration {

    @Autowired
    MetricsProperties metricsProperties;

    @Autowired
    GraphiteClientProperties graphiteClientProperties;


    @Bean
    public MetricsPaths metricsPaths() {
        return new MetricsPaths(metricsProperties.getPrefix());
    }

    @Bean
    public GraphiteClient graphiteClient(@Qualifier("graphiteRestTemplate") RestTemplate graphiteRestTemplate) {
        RestTemplateGraphiteClient underlyingGraphiteClient =
                new RestTemplateGraphiteClient(graphiteRestTemplate, URI.create(metricsProperties.getGraphiteHttpUri()));
        return new CachingGraphiteClient(
                underlyingGraphiteClient,
                systemTicker(),
                graphiteClientProperties.getCacheTtlSeconds(),
                graphiteClientProperties.getCacheSize()
        );
    }

    @Bean("graphiteRestTemplate")
    public RestTemplate restTemplate(GraphiteClientProperties properties) {
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
}
