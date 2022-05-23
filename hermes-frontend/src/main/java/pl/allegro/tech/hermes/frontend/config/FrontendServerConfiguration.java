package pl.allegro.tech.hermes.frontend.config;

import io.undertow.server.HttpHandler;
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

import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_STARTUP_TOPIC_METADATA_LOADING_ENABLED;

@Configuration
public class FrontendServerConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public HermesServer hermesServer(ConfigFactory configFactory,
                                     HermesMetrics hermesMetrics,
                                     HttpHandler publishingHandler,
                                     DefaultReadinessChecker defaultReadinessChecker,
                                     DefaultMessagePreviewPersister defaultMessagePreviewPersister,
                                     ThroughputLimiter throughputLimiter,
                                     TopicMetadataLoadingJob topicMetadataLoadingJob,
                                     SslContextFactoryProvider sslContextFactoryProvider) {
        return new HermesServer(configFactory, hermesMetrics, publishingHandler, defaultReadinessChecker,
                defaultMessagePreviewPersister, throughputLimiter, topicMetadataLoadingJob, sslContextFactoryProvider);
    }

    @Bean
    public DefaultReadinessChecker readinessChecker(ConfigFactory config,
                                                    TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                                                    ReadinessRepository readinessRepository) {
        return new DefaultReadinessChecker(config, topicMetadataLoadingRunner, readinessRepository);
    }

    @Bean
    public SslContextFactoryProvider sslContextFactoryProvider(Optional<SslContextFactory> sslContextFactory,
                                                               ConfigFactory configFactory) {
        return new SslContextFactoryProvider(sslContextFactory.orElse(null), configFactory);
    }

    @Bean
    public TopicMetadataLoadingJob topicMetadataLoadingJob(TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                                                           ConfigFactory config) {
        return new TopicMetadataLoadingJob(topicMetadataLoadingRunner, config);
    }

    @Bean
    public TopicMetadataLoadingRunner topicMetadataLoadingRunner(BrokerMessageProducer brokerMessageProducer,
                                                                 TopicsCache topicsCache,
                                                                 ConfigFactory config) {
        return new TopicMetadataLoadingRunner(brokerMessageProducer, topicsCache, config);
    }

    @Bean(initMethod = "run")
    public TopicMetadataLoadingStartupHook topicMetadataLoadingStartupHook(TopicMetadataLoadingRunner topicMetadataLoadingRunner, ConfigFactory configFactory) {
        return new TopicMetadataLoadingStartupHook(topicMetadataLoadingRunner, configFactory.getBooleanProperty(FRONTEND_STARTUP_TOPIC_METADATA_LOADING_ENABLED));
    }

    @Bean(initMethod = "run")
    public TopicSchemaLoadingStartupHook topicSchemaLoadingStartupHook(TopicsCache topicsCache,
                                                                       SchemaRepository schemaRepository,
                                                                       ConfigFactory config) {
        return new TopicSchemaLoadingStartupHook(topicsCache, schemaRepository, config);
    }
}
