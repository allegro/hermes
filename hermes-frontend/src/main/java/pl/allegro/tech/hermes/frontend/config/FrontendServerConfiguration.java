package pl.allegro.tech.hermes.frontend.config;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.undertow.server.HttpHandler;
import jakarta.inject.Named;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter;
import pl.allegro.tech.hermes.frontend.publishing.preview.DefaultMessagePreviewPersister;
import pl.allegro.tech.hermes.frontend.readiness.DefaultReadinessChecker;
import pl.allegro.tech.hermes.frontend.readiness.HealthCheckService;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.frontend.server.SslContextFactoryProvider;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingJob;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingRunner;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingStartupHook;
import pl.allegro.tech.hermes.frontend.server.TopicSchemaLoadingStartupHook;
import pl.allegro.tech.hermes.schema.SchemaRepository;

import java.util.Optional;

@Configuration
@EnableConfigurationProperties({
        TopicLoadingProperties.class,
        SslProperties.class,
        HermesServerProperties.class
})
public class FrontendServerConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public HermesServer hermesServer(HermesServerProperties hermesServerProperties,
                                     SslProperties sslProperties,
                                     MetricsFacade metricsFacade,
                                     HttpHandler publishingHandler,
                                     HealthCheckService healthCheckService,
                                     DefaultReadinessChecker defaultReadinessChecker,
                                     DefaultMessagePreviewPersister defaultMessagePreviewPersister,
                                     ThroughputLimiter throughputLimiter,
                                     TopicMetadataLoadingJob topicMetadataLoadingJob,
                                     SslContextFactoryProvider sslContextFactoryProvider,
                                     TopicLoadingProperties topicLoadingProperties,
                                     PrometheusMeterRegistry prometheusMeterRegistry) {
        return new HermesServer(
                sslProperties,
                hermesServerProperties,
                metricsFacade,
                publishingHandler,
                healthCheckService,
                defaultReadinessChecker,
                defaultMessagePreviewPersister,
                throughputLimiter,
                topicMetadataLoadingJob,
                topicLoadingProperties.getMetadataRefreshJob().isEnabled(),
                sslContextFactoryProvider,
                prometheusMeterRegistry);
    }

    @Bean
    public SslContextFactoryProvider sslContextFactoryProvider(Optional<SslContextFactory> sslContextFactory,
                                                               SslProperties sslProperties) {
        return new SslContextFactoryProvider(sslContextFactory.orElse(null), sslProperties);
    }

    @Bean
    public TopicMetadataLoadingJob topicMetadataLoadingJob(TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                                                           TopicLoadingProperties topicLoadingProperties) {
        return new TopicMetadataLoadingJob(topicMetadataLoadingRunner, topicLoadingProperties.getMetadataRefreshJob().getInterval());
    }

    @Bean
    public TopicMetadataLoadingRunner topicMetadataLoadingRunner(@Named("kafkaBrokerMessageProducer") BrokerMessageProducer brokerMessageProducer,
                                                                 TopicsCache topicsCache,
                                                                 TopicLoadingProperties topicLoadingProperties) {
        return new TopicMetadataLoadingRunner(brokerMessageProducer, topicsCache,
                topicLoadingProperties.getMetadata().getRetryCount(),
                topicLoadingProperties.getMetadata().getRetryInterval(),
                topicLoadingProperties.getMetadata().getThreadPoolSize());
    }

    @Bean(initMethod = "run")
    public TopicMetadataLoadingStartupHook topicMetadataLoadingStartupHook(TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                                                                           TopicLoadingProperties topicLoadingProperties) {
        return new TopicMetadataLoadingStartupHook(topicMetadataLoadingRunner, topicLoadingProperties.getMetadata().isEnabled());
    }

    @Bean(initMethod = "run")
    public TopicSchemaLoadingStartupHook topicSchemaLoadingStartupHook(TopicsCache topicsCache,
                                                                       SchemaRepository schemaRepository,
                                                                       TopicLoadingProperties topicLoadingProperties) {
        return new TopicSchemaLoadingStartupHook(topicsCache, schemaRepository,
                topicLoadingProperties.getSchema().getRetryCount(),
                topicLoadingProperties.getSchema().getThreadPoolSize(),
                topicLoadingProperties.getSchema().isEnabled());
    }
}
