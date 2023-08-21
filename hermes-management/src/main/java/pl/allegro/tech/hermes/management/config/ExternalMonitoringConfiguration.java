package pl.allegro.tech.hermes.management.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.management.infrastructure.graphite.CachingGraphiteClient;
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteClient;
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteMetricsProvider;
import pl.allegro.tech.hermes.management.infrastructure.graphite.RestTemplateGraphiteClient;
import pl.allegro.tech.hermes.management.infrastructure.prometheus.CachingPrometheusClient;
import pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusClient;
import pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusMetricsProvider;
import pl.allegro.tech.hermes.management.infrastructure.prometheus.RestTemplatePrometheusClient;
import pl.allegro.tech.hermes.management.stub.MetricsPaths;

import java.net.URI;

import static com.google.common.base.Ticker.systemTicker;

@Configuration
@EnableConfigurationProperties({MetricsProperties.class})
public class ExternalMonitoringConfiguration {

    @Autowired
    MetricsProperties metricsProperties;

    @Autowired
    MonitoringClientProperties graphiteClientProperties;


    @Bean
    @ConditionalOnProperty(value = "graphite.client.enabled", havingValue = "true")
    public GraphiteMetricsProvider graphiteMetricsProvider(GraphiteClient graphiteClient, MetricsPaths paths) {
        return new GraphiteMetricsProvider(graphiteClient, paths);
    }

    @Bean
    @ConditionalOnProperty(value = "prometheus.client.enabled", havingValue = "true")
    public PrometheusMetricsProvider prometheusMetricsProvider(PrometheusClient prometheusClient) {
        return new PrometheusMetricsProvider(prometheusClient, "hermes_consumers", "hermes_frontend");
    }


    @Bean
    public MetricsPaths metricsPaths() {
        return new MetricsPaths(metricsProperties.getPrefix());
    }

    @Bean
    @ConditionalOnProperty(value = "graphite.client.enabled", havingValue = "true")
    public GraphiteClient graphiteClient(@Qualifier("monitoringRestTemplate") RestTemplate graphiteRestTemplate) {
        RestTemplateGraphiteClient underlyingGraphiteClient =
                new RestTemplateGraphiteClient(graphiteRestTemplate, URI.create(metricsProperties.getGraphiteHttpUri()));
        return new CachingGraphiteClient(
                underlyingGraphiteClient,
                systemTicker(),
                graphiteClientProperties.getCacheTtlSeconds(),
                graphiteClientProperties.getCacheSize()
        );
    }

    @Bean
    @ConditionalOnProperty(value = "prometheus.client.enabled", havingValue = "true")
    public PrometheusClient prometheusClient(@Qualifier("monitoringRestTemplate") RestTemplate graphiteRestTemplate,
                                             MonitoringClientProperties clientProperties) {
        RestTemplatePrometheusClient underlyingPrometheusClient =
                new RestTemplatePrometheusClient(graphiteRestTemplate, URI.create(clientProperties.getExternalMonitoringUrl()));
        return new CachingPrometheusClient(
                underlyingPrometheusClient,
                systemTicker(),
                graphiteClientProperties.getCacheTtlSeconds(),
                graphiteClientProperties.getCacheSize()
        );
    }

    @Bean("monitoringRestTemplate")
    public RestTemplate restTemplate(MonitoringClientProperties clientProperties) {
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(clientProperties.getMaxConnections())
                .setMaxConnPerRoute(clientProperties.getMaxConnectionsPerRoute())
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(clientProperties.getConnectionTimeoutMillis()))
                .setResponseTimeout(Timeout.ofMilliseconds(clientProperties.getSocketTimeoutMillis()))
                .build();

        HttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                .build();

        ClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(client);

        return new RestTemplate(clientHttpRequestFactory);
    }
}
