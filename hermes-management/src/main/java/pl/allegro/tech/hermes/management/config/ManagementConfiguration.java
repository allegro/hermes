package pl.allegro.tech.hermes.management.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.clock.ClockFactory;
import pl.allegro.tech.hermes.common.di.factories.MicrometerRegistryParameters;
import pl.allegro.tech.hermes.common.di.factories.PrometheusMeterRegistryFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.util.InetAddressInstanceIdResolver;
import pl.allegro.tech.hermes.common.util.InstanceIdResolver;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionLagSource;
import pl.allegro.tech.hermes.management.infrastructure.metrics.NoOpSubscriptionLagSource;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({
        TopicProperties.class,
        MetricsProperties.class,
        HttpClientProperties.class,
        ConsistencyCheckerProperties.class,
        PrometheusProperties.class,
        MicrometerRegistryProperties.class
})
public class ManagementConfiguration {

    @Autowired
    TopicProperties topicProperties;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        mapper.registerModule(new JavaTimeModule());

        final InjectableValues defaultSchemaIdAwareSerializationEnabled = new InjectableValues.Std().addValue(
                Topic.DEFAULT_SCHEMA_ID_SERIALIZATION_ENABLED_KEY,
                topicProperties.isDefaultSchemaIdAwareSerializationEnabled());

        mapper.setInjectableValues(defaultSchemaIdAwareSerializationEnabled);

        return mapper;
    }

    @Bean
    @ConditionalOnMissingBean
    public MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public PrometheusMeterRegistry micrometerRegistry(MicrometerRegistryParameters micrometerRegistryParameters,
                                                      PrometheusConfig prometheusConfig) {
        return new PrometheusMeterRegistryFactory(micrometerRegistryParameters,
                prometheusConfig, "hermes-management").provide();
    }

    @Bean
    @ConditionalOnMissingBean
    PrometheusConfig prometheusConfig(PrometheusProperties properties) {
        return new PrometheusConfigAdapter(properties);
    }

    @Bean
    public InstanceIdResolver instanceIdResolver() {
        return new InetAddressInstanceIdResolver();
    }

    @Bean
    public PathsCompiler pathsCompiler(InstanceIdResolver instanceIdResolver) {
        return new PathsCompiler(instanceIdResolver.resolve());
    }

    @Bean
    public HermesMetrics hermesMetrics(MetricRegistry metricRegistry,
                                       PathsCompiler pathsCompiler) {
        return new HermesMetrics(metricRegistry, pathsCompiler);
    }

    @Bean
    public MetricsFacade micrometerHermesMetrics(MeterRegistry meterRegistry, HermesMetrics hermesMetrics) {
        return new MetricsFacade(meterRegistry, hermesMetrics);
    }

    @Bean
    @ConditionalOnMissingBean
    public SubscriptionLagSource consumerLagSource() {
        return new NoOpSubscriptionLagSource();
    }

    @Bean
    public Clock clock() {
        return new ClockFactory().provide();
    }


}
