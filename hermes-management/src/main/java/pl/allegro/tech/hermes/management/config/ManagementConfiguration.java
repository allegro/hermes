package pl.allegro.tech.hermes.management.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.clock.ClockFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.util.InetAddressInstanceIdResolver;
import pl.allegro.tech.hermes.common.util.InstanceIdResolver;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionLagSource;
import pl.allegro.tech.hermes.management.infrastructure.leader.ManagementLeadership;
import pl.allegro.tech.hermes.management.infrastructure.metrics.NoOpSubscriptionLagSource;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

@Configuration
@EnableConfigurationProperties({
  TopicProperties.class,
  HttpClientProperties.class,
  ConsistencyCheckerProperties.class,
  PrometheusProperties.class,
  MicrometerRegistryProperties.class,
})
public class ManagementConfiguration {

  @Autowired TopicProperties topicProperties;

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
    mapper.registerModules(
        new JavaTimeModule(),
        new Jdk8Module()); // Jdk8Module is required for Jackson to serialize & deserialize Optional
    // type

    final InjectableValues defaultSchemaIdAwareSerializationEnabled =
        new InjectableValues.Std()
            .addValue(
                Topic.DEFAULT_SCHEMA_ID_SERIALIZATION_ENABLED_KEY,
                topicProperties.isDefaultSchemaIdAwareSerializationEnabled())
            .addValue(
                Topic.DEFAULT_FALLBACK_TO_REMOTE_DATACENTER_KEY,
                topicProperties.isDefaultFallbackToRemoteDatacenterEnabled());

    mapper.setInjectableValues(defaultSchemaIdAwareSerializationEnabled);

    return mapper;
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
  public MetricsFacade micrometerHermesMetrics(MeterRegistry meterRegistry) {
    return new MetricsFacade(meterRegistry);
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
  ManagementLeadership managementLeadership(
      ZookeeperClientManager zookeeperClientManager,
      @Value("${management.leadership.zookeeper-dc}") String leaderElectionDc,
      ZookeeperPaths zookeeperPaths) {
    return new ManagementLeadership(zookeeperClientManager, leaderElectionDc, zookeeperPaths);
  }
}
