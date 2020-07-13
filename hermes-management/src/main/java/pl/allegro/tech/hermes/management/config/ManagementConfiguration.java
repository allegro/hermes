package pl.allegro.tech.hermes.management.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import static javax.servlet.DispatcherType.REQUEST;

import com.netflix.config.DynamicPropertyFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.clock.ClockFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.management.api.ReadOnlyFilter;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionLagSource;
import pl.allegro.tech.hermes.management.infrastructure.metrics.NoOpSubscriptionLagSource;

import javax.servlet.DispatcherType;
import java.time.Clock;
import java.util.EnumSet;

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

    @Bean
    public FilterRegistrationBean<ReadOnlyFilter> readOnlyFilter(ModeService modeService) {
        FilterRegistrationBean<ReadOnlyFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setDispatcherTypes(REQUEST);
        registrationBean.setFilter(new ReadOnlyFilter(modeService));
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }

    @Bean
    public ConfigFactory configFactory() {
        return new ConfigFactory(DynamicPropertyFactory.getInstance());
    }
}
