package pl.allegro.tech.hermes.management.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.clock.ClockFactory;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionLagSource;
import pl.allegro.tech.hermes.management.infrastructure.metrics.NoOpSubscriptionLagSource;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({TopicProperties.class, MetricsProperties.class, HttpClientProperties.class})
public class ManagementConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    @ConditionalOnMissingBean
    public MetricRegistry metricRegistry() {
        return new MetricRegistry();
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
