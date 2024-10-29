package pl.allegro.tech.hermes.frontend.config;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.undertow.server.HttpHandler;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter;
import pl.allegro.tech.hermes.frontend.publishing.preview.DefaultMessagePreviewPersister;
import pl.allegro.tech.hermes.frontend.readiness.HealthCheckService;
import pl.allegro.tech.hermes.frontend.readiness.ReadinessChecker;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.frontend.server.SslContextFactoryProvider;
import pl.allegro.tech.hermes.frontend.server.TopicSchemaLoadingStartupHook;
import pl.allegro.tech.hermes.schema.SchemaRepository;

@Configuration
@EnableConfigurationProperties({
  TopicLoadingProperties.class,
  ReadinessCheckProperties.class,
  SslProperties.class,
  HermesServerProperties.class
})
public class FrontendServerConfiguration {

  @Bean(initMethod = "start", destroyMethod = "stop")
  public HermesServer hermesServer(
      HermesServerProperties hermesServerProperties,
      SslProperties sslProperties,
      MetricsFacade metricsFacade,
      HttpHandler publishingHandler,
      HealthCheckService healthCheckService,
      ReadinessChecker readinessChecker,
      DefaultMessagePreviewPersister defaultMessagePreviewPersister,
      ThroughputLimiter throughputLimiter,
      SslContextFactoryProvider sslContextFactoryProvider,
      PrometheusMeterRegistry prometheusMeterRegistry) {
    return new HermesServer(
        sslProperties,
        hermesServerProperties,
        metricsFacade,
        publishingHandler,
        healthCheckService,
        readinessChecker,
        defaultMessagePreviewPersister,
        throughputLimiter,
        sslContextFactoryProvider,
        prometheusMeterRegistry);
  }

  @Bean
  public SslContextFactoryProvider sslContextFactoryProvider(
      Optional<SslContextFactory> sslContextFactory, SslProperties sslProperties) {
    return new SslContextFactoryProvider(sslContextFactory.orElse(null), sslProperties);
  }

  @Bean(initMethod = "run")
  public TopicSchemaLoadingStartupHook topicSchemaLoadingStartupHook(
      TopicsCache topicsCache,
      SchemaRepository schemaRepository,
      TopicLoadingProperties topicLoadingProperties) {
    return new TopicSchemaLoadingStartupHook(
        topicsCache,
        schemaRepository,
        topicLoadingProperties.getSchema().getRetryCount(),
        topicLoadingProperties.getSchema().getThreadPoolSize(),
        topicLoadingProperties.getSchema().isEnabled());
  }
}
