package pl.allegro.tech.hermes.frontend.config;

import io.undertow.server.HttpHandler;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;
import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewPersister;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.frontend.server.ReadinessChecker;
import pl.allegro.tech.hermes.frontend.server.SslContextFactoryProvider;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingJob;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingRunner;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingStartupHook;
import pl.allegro.tech.hermes.frontend.server.TopicSchemaLoadingStartupHook;
import pl.allegro.tech.hermes.frontend.services.HealthCheckService;
import pl.allegro.tech.hermes.schema.SchemaRepository;

import java.util.Optional;

@Configuration
public class FrontendServerConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public HermesServer hermesServer(ConfigFactory configFactory,
                                     HermesMetrics hermesMetrics,
                                     HttpHandler publishingHandler,
                                     ReadinessChecker readinessChecker,
                                     MessagePreviewPersister messagePreviewPersister,
                                     ThroughputLimiter throughputLimiter,
                                     TopicMetadataLoadingJob topicMetadataLoadingJob,
                                     SslContextFactoryProvider sslContextFactoryProvider) {
        return new HermesServer(configFactory, hermesMetrics, publishingHandler, readinessChecker,
                messagePreviewPersister, throughputLimiter, topicMetadataLoadingJob, sslContextFactoryProvider);
    }

    @Bean
    public ReadinessChecker readinessChecker(ConfigFactory config,
                                             TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                                             ReadinessRepository readinessRepository) {
        return new ReadinessChecker(config, topicMetadataLoadingRunner, readinessRepository);
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
    @Conditional(TopicMetadataLoadingStartupHookCondition.class)//TODO: eventually change to ConditionalOnProperty
    public TopicMetadataLoadingStartupHook topicMetadataLoadingStartupHook(TopicMetadataLoadingRunner topicMetadataLoadingRunner) {
        return new TopicMetadataLoadingStartupHook(topicMetadataLoadingRunner);
    }

    @Bean(initMethod = "run")
    @Conditional(TopicSchemaLoadingStartupHookCondition.class)//TODO: eventually change to ConditionalOnProperty
    public TopicSchemaLoadingStartupHook topicSchemaLoadingStartupHook(TopicsCache topicsCache,
                                                                       SchemaRepository schemaRepository,
                                                                       ConfigFactory config) {
        return new TopicSchemaLoadingStartupHook(topicsCache, schemaRepository, config);
    }
}
