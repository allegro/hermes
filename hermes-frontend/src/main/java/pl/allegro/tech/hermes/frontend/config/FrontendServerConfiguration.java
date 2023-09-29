package pl.allegro.tech.hermes.frontend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.undertow.server.HttpHandler;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaPartitionLeaderLoadingJob;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter;
import pl.allegro.tech.hermes.frontend.publishing.preview.DefaultMessagePreviewPersister;
import pl.allegro.tech.hermes.frontend.server.DefaultReadinessChecker;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.frontend.server.SslContextFactoryProvider;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingJob;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingRunner;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingStartupHook;
import pl.allegro.tech.hermes.frontend.server.TopicSchemaLoadingStartupHook;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.schema.SchemaRepository;

import java.util.Optional;

@Configuration
@EnableConfigurationProperties({
        TopicLoadingProperties.class,
        ReadinessCheckProperties.class,
        SslProperties.class,
        HermesServerProperties.class
})
public class FrontendServerConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public HermesServer hermesServer(HermesServerProperties hermesServerProperties,
                                     SslProperties sslProperties,
                                     MetricsFacade metricsFacade,
                                     HttpHandler publishingHandler,
                                     DefaultReadinessChecker defaultReadinessChecker,
                                     DefaultMessagePreviewPersister defaultMessagePreviewPersister,
                                     ThroughputLimiter throughputLimiter,
                                     TopicMetadataLoadingJob topicMetadataLoadingJob,
                                     SslContextFactoryProvider sslContextFactoryProvider,
                                     TopicLoadingProperties topicLoadingProperties,
                                     PrometheusMeterRegistry prometheusMeterRegistry,
                                     KafkaPartitionLeaderLoadingJob kafkaPartitionLeaderLoadingJob,
                                     BrokerLatencyReporterProperties brokerLatencyReporterProperties) {
        return new HermesServer(
                sslProperties,
                hermesServerProperties,
                metricsFacade,
                publishingHandler,
                defaultReadinessChecker,
                defaultMessagePreviewPersister,
                throughputLimiter,
                topicMetadataLoadingJob,
                topicLoadingProperties.getMetadataRefreshJob().isEnabled(),
                sslContextFactoryProvider,
                prometheusMeterRegistry,
                kafkaPartitionLeaderLoadingJob,
                brokerLatencyReporterProperties.isPerBrokerLatencyReportingEnabled()
                );
    }

    @Bean
    public DefaultReadinessChecker readinessChecker(ReadinessCheckProperties readinessCheckProperties,
                                                    TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                                                    CuratorFramework zookeeper,
                                                    ZookeeperPaths paths,
                                                    ObjectMapper mapper) {
        return new DefaultReadinessChecker(
                topicMetadataLoadingRunner,
                zookeeper,
                paths,
                mapper,
                readinessCheckProperties.isEnabled(),
                readinessCheckProperties.isKafkaCheckEnabled(),
                readinessCheckProperties.getInterval()
        );
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
    public TopicMetadataLoadingRunner topicMetadataLoadingRunner(BrokerMessageProducer brokerMessageProducer,
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
