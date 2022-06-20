package pl.allegro.tech.hermes.frontend.config;

import io.undertow.server.HttpHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;
import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter;
import pl.allegro.tech.hermes.frontend.publishing.preview.DefaultMessagePreviewPersister;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.frontend.server.DefaultReadinessChecker;
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
        ReadinessCheckProperties.class
})
public class FrontendServerConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public HermesServer hermesServer(ConfigFactory configFactory,
                                     HermesMetrics hermesMetrics,
                                     HttpHandler publishingHandler,
                                     DefaultReadinessChecker defaultReadinessChecker,
                                     DefaultMessagePreviewPersister defaultMessagePreviewPersister,
                                     ThroughputLimiter throughputLimiter,
                                     TopicMetadataLoadingJob topicMetadataLoadingJob,
                                     SslContextFactoryProvider sslContextFactoryProvider,
                                     TopicLoadingProperties topicLoadingProperties) {
        return new HermesServer(configFactory, hermesMetrics, publishingHandler, defaultReadinessChecker,
                defaultMessagePreviewPersister, throughputLimiter, topicMetadataLoadingJob, topicLoadingProperties.getMetadataRefreshJob().isEnabled(), sslContextFactoryProvider);
    }

    @Bean
    public DefaultReadinessChecker readinessChecker(ReadinessCheckProperties readinessCheckProperties,
                                                    TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                                                    ReadinessRepository readinessRepository) {
        return new DefaultReadinessChecker(topicMetadataLoadingRunner, readinessRepository, readinessCheckProperties.isEnabled(), readinessCheckProperties.getIntervalSeconds());
    }

    @Bean
    public SslContextFactoryProvider sslContextFactoryProvider(Optional<SslContextFactory> sslContextFactory,
                                                               ConfigFactory configFactory) {
        return new SslContextFactoryProvider(sslContextFactory.orElse(null), configFactory);
    }

    @Bean
    public TopicMetadataLoadingJob topicMetadataLoadingJob(TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                                                           TopicLoadingProperties topicLoadingProperties) {
        return new TopicMetadataLoadingJob(topicMetadataLoadingRunner, topicLoadingProperties.getMetadataRefreshJob().getIntervalSeconds());
    }

    @Bean
    public TopicMetadataLoadingRunner topicMetadataLoadingRunner(BrokerMessageProducer brokerMessageProducer,
                                                                 TopicsCache topicsCache,
                                                                 TopicLoadingProperties topicLoadingProperties) {
        return new TopicMetadataLoadingRunner(brokerMessageProducer, topicsCache,
                topicLoadingProperties.getMetadata().getRetryCount(),
                topicLoadingProperties.getMetadata().getRetryInterval(),
                topicLoadingProperties.getMetadata().getThreadPoolSize());
    }

    @Bean(initMethod = "run")
    public TopicMetadataLoadingStartupHook topicMetadataLoadingStartupHook(TopicMetadataLoadingRunner topicMetadataLoadingRunner, TopicLoadingProperties topicLoadingProperties) {
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
