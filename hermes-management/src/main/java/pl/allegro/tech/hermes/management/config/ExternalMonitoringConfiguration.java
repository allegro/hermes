package pl.allegro.tech.hermes.management.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.management.infrastructure.prometheus.CachingPrometheusClient;
import pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusClient;
import pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusMetricsProvider;
import pl.allegro.tech.hermes.management.infrastructure.prometheus.RestTemplateParallelPrometheusClient;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Ticker.systemTicker;

@Configuration
public class ExternalMonitoringConfiguration {

    @Bean
    @ConditionalOnProperty(value = "prometheus.client.enabled", havingValue = "true")
    public PrometheusMetricsProvider prometheusMetricsProvider(PrometheusClient prometheusClient,
                                                               PrometheusMonitoringClientProperties properties) {
        return new PrometheusMetricsProvider(prometheusClient,
                properties.getConsumersMetricsPrefix(), properties.getFrontendMetricsPrefix(),
                properties.getAdditionalFilters());
    }

    @Bean
    @ConditionalOnProperty(value = "prometheus.client.enabled", havingValue = "true")
    public PrometheusClient prometheusClient(@Qualifier("monitoringRestTemplate") RestTemplate monitoringRestTemplate,
                                             PrometheusMonitoringClientProperties clientProperties,
                                             @Qualifier("prometheusFetcherExecutorService") ExecutorService executorService,
                                             MeterRegistry meterRegistry) {
        RestTemplateParallelPrometheusClient underlyingPrometheusClient =
                new RestTemplateParallelPrometheusClient(
                        monitoringRestTemplate,
                        URI.create(clientProperties.getExternalMonitoringUrl()),
                        executorService,
                        Duration.ofMillis(clientProperties.getParallelFetchingTimeoutMillis()),
                        meterRegistry);
        return new CachingPrometheusClient(
                underlyingPrometheusClient,
                systemTicker(),
                clientProperties.getCacheTtlSeconds(),
                clientProperties.getCacheSize()
        );
    }

    @Bean("monitoringRestTemplate")
    @ConditionalOnMissingBean(name = "monitoringRestTemplate")
    @ConditionalOnProperty(value = "prometheus.client.enabled", havingValue = "true")
    public RestTemplate restTemplate(ExternalMonitoringClientProperties clientProperties) {
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

    @Bean("prometheusFetcherExecutorService")
    @ConditionalOnMissingBean(name = "prometheusFetcherExecutorService")
    @ConditionalOnProperty(value = "prometheus.client.enabled", havingValue = "true")
    ExecutorService executorService(ExternalMonitoringClientProperties clientProperties) {
        return Executors.newFixedThreadPool(clientProperties.getParallelFetchingThreads(),
                new ThreadFactoryBuilder().setNameFormat("prometheus-metrics-fetcher-%d").build()
        );
    }
}
