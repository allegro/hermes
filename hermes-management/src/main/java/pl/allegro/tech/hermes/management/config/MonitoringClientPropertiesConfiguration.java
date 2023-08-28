package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
These properties beans must be in different configuration class than ExternalMonitoringConfiguration.java. It allows
avoiding circular dependencies between beans.
 */
@Configuration
public class MonitoringClientPropertiesConfiguration {
    @Bean
    @ConfigurationProperties("graphite.client")
    @ConditionalOnProperty(value = "graphite.client.enabled", havingValue = "true")
    public ExternalMonitoringClientProperties graphiteMonitoringClientProperties() {
        return new ExternalMonitoringClientProperties();
    }

    @Bean
    @ConfigurationProperties("prometheus.client")
    @ConditionalOnProperty(value = "prometheus.client.enabled", havingValue = "true")
    public ExternalMonitoringClientProperties prometheusMonitoringClientProperties() {
        return new ExternalMonitoringClientProperties();
    }
}
