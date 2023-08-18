package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
These properties beans must be at different configuration class than ExternalMonitoringConfiguration.java to avoid
circular dependencies.
 */
@Configuration
public class MonitoringClientPropertiesConfiguration {
    @Bean
    @ConfigurationProperties("graphite.client")
    @ConditionalOnProperty(value = "graphite.client.enabled", havingValue = "true")
    public MonitoringClientProperties graphiteMonitoringClientProperties() {
        return new MonitoringClientProperties();
    }

    @Bean
    @ConfigurationProperties("prometheus.client")
    @ConditionalOnProperty(value = "prometheus.client.enabled", havingValue = "true")
    public MonitoringClientProperties prometheusMonitoringClientProperties() {
        return new MonitoringClientProperties();
    }
}
