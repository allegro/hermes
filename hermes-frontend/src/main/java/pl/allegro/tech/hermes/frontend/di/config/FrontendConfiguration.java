package pl.allegro.tech.hermes.frontend.di.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.hook.HooksHandler;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.frontend.blacklist.BlacklistZookeeperNotifyingCache;
import pl.allegro.tech.hermes.frontend.buffer.BackupMessagesLoader;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCacheFactory;
import pl.allegro.tech.hermes.frontend.di.BlacklistZookeeperNotifyingCacheFactory;
import pl.allegro.tech.hermes.frontend.di.PersistentBufferExtension;
import pl.allegro.tech.hermes.frontend.di.ReadinessRepositoryFactory;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaBrokerMessageProducerFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaHeaderFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageProducerFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaTopicMetadataFetcher;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaTopicMetadataFetcherFactory;
import pl.allegro.tech.hermes.frontend.producer.kafka.Producers;
import pl.allegro.tech.hermes.frontend.publishing.handlers.HandlersChainFactory;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiter;
import pl.allegro.tech.hermes.frontend.publishing.handlers.ThroughputLimiterFactory;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageEndProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.AvroEnforcer;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageContentTypeEnforcer;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageFactory;
import pl.allegro.tech.hermes.frontend.publishing.metadata.DefaultHeadersPropagator;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewFactory;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewLog;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewPersister;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.frontend.server.ReadinessChecker;
import pl.allegro.tech.hermes.frontend.server.SslContextFactoryProvider;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingJob;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingRunner;
import pl.allegro.tech.hermes.frontend.server.TopicMetadataLoadingStartupHook;
import pl.allegro.tech.hermes.frontend.server.TopicSchemaLoadingStartupHook;
import pl.allegro.tech.hermes.frontend.server.auth.AuthenticationConfigurationProvider;
import pl.allegro.tech.hermes.frontend.services.HealthCheckService;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import pl.allegro.tech.hermes.frontend.validator.TopicMessageValidator;
import pl.allegro.tech.hermes.frontend.validator.TopicMessageValidatorListFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.tracker.frontend.LogRepository;
import pl.allegro.tech.hermes.tracker.frontend.NoOperationPublishingTracker;
import pl.allegro.tech.hermes.tracker.frontend.PublishingMessageTracker;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import javax.inject.Named;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class FrontendConfiguration {

    @Bean
    public ApplicationContext applicationContext() {
        return new GenericApplicationContext();
    }

    @Bean
    public HermesServer hermesServer(ConfigFactory configFactory,
                                     HermesMetrics hermesMetrics,
                                     HttpHandler publishingHandler,
                                     HealthCheckService healthCheckService,
                                     ReadinessChecker readinessChecker,
                                     MessagePreviewPersister messagePreviewPersister,
                                     ThroughputLimiter throughputLimiter,
                                     TopicMetadataLoadingJob topicMetadataLoadingJob,
                                     SslContextFactoryProvider sslContextFactoryProvider) {
        return new HermesServer(configFactory, hermesMetrics, publishingHandler, healthCheckService, readinessChecker,
                messagePreviewPersister, throughputLimiter, topicMetadataLoadingJob, sslContextFactoryProvider);
    }

    @Bean
    public MessageErrorProcessor messageErrorProcessor(ObjectMapper objectMapper, Trackers trackers) {
        return new MessageErrorProcessor(objectMapper, trackers);
    }

    @Bean
    public MessageEndProcessor messageEndProcessor(Trackers trackers, BrokerListeners brokerListeners) {
        return new MessageEndProcessor(trackers, brokerListeners);
    }

    @Bean
    public MessageValidators messageValidators(List<TopicMessageValidator> messageValidators) {
        return new MessageValidators(messageValidators);
    }

    @Bean
    public TopicMetadataLoadingRunner topicMetadataLoadingRunner(BrokerMessageProducer brokerMessageProducer,
                                                                 TopicsCache topicsCache,
                                                                 ConfigFactory config) {
        return new TopicMetadataLoadingRunner(brokerMessageProducer, topicsCache, config);
    }

    @Bean
    public TopicMetadataLoadingJob topicMetadataLoadingJob(TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                                                           ConfigFactory config) {
        return new TopicMetadataLoadingJob(topicMetadataLoadingRunner, config);
    }

    @Bean
    public TopicMetadataLoadingStartupHook topicMetadataLoadingStartupHook(TopicMetadataLoadingRunner topicMetadataLoadingRunner) {
        return new TopicMetadataLoadingStartupHook(topicMetadataLoadingRunner);
    }

    @Bean
    public TopicSchemaLoadingStartupHook topicSchemaLoadingStartupHook(TopicsCache topicsCache,
                                                                       SchemaRepository schemaRepository,
                                                                       ConfigFactory config) {
        return new TopicSchemaLoadingStartupHook(topicsCache, schemaRepository, config);
    }

    @Bean
    public ReadinessChecker readinessChecker(ConfigFactory config,
                                             TopicMetadataLoadingRunner topicMetadataLoadingRunner,
                                             ReadinessRepository readinessRepository) {
        return new ReadinessChecker(config, topicMetadataLoadingRunner, readinessRepository);
    }

    @Bean
    public HeadersPropagator defaultHeadersPropagator(ConfigFactory config) {
        return new DefaultHeadersPropagator(config);
    }

    @Bean
    public HttpHandler httpHandler(TopicsCache topicsCache, MessageErrorProcessor messageErrorProcessor,
                                   MessageEndProcessor messageEndProcessor, ConfigFactory configFactory, MessageFactory messageFactory,
                                   BrokerMessageProducer brokerMessageProducer, MessagePreviewLog messagePreviewLog,
                                   ThroughputLimiter throughputLimiter, AuthenticationConfigurationProvider authConfigProvider) {
        return new HandlersChainFactory(topicsCache, messageErrorProcessor, messageEndProcessor, configFactory, messageFactory,
                brokerMessageProducer, messagePreviewLog, throughputLimiter, authConfigProvider).provide();
    }

    @Bean(destroyMethod = "close")
    public Producers kafkaMessageProducer(ConfigFactory configFactory) {
        return new KafkaMessageProducerFactory(configFactory).provide();
    }

    @Bean(destroyMethod = "close")
    public KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher(ConfigFactory configFactory) {
        return new KafkaTopicMetadataFetcherFactory(configFactory).provide();
    }

    @Bean
    public BrokerMessageProducer kafkaBrokerMessageProducer(Producers producers,
                                                            KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher,
                                                            HermesMetrics hermesMetrics,
                                                            KafkaHeaderFactory kafkaHeaderFactory,
                                                            ConfigFactory configFactory) {
        return new KafkaBrokerMessageProducerFactory(producers, kafkaTopicMetadataFetcher, hermesMetrics,
                kafkaHeaderFactory, configFactory).provide();
    }

    @Bean
    public ThroughputLimiter throughputLimiter(ConfigFactory configs, HermesMetrics hermesMetrics) {
        return new ThroughputLimiterFactory(configs, hermesMetrics).provide();
    }

    @Bean
    public PublishingMessageTracker publishingMessageTracker(List<LogRepository> repositories, Clock clock) {
        return new PublishingMessageTracker(repositories, clock);
    }

    @Bean
    public NoOperationPublishingTracker noOperationPublishingTracker() {
        return new NoOperationPublishingTracker();
    }

    @Bean
    public TopicsCache topicsCacheFactory(InternalNotificationsBus internalNotificationsBus,
                                          GroupRepository groupRepository,
                                          TopicRepository topicRepository,
                                          HermesMetrics hermesMetrics,
                                          KafkaNamesMapper kafkaNamesMapper,
                                          BlacklistZookeeperNotifyingCache blacklistZookeeperNotifyingCache) {
        return new TopicsCacheFactory(internalNotificationsBus, groupRepository, topicRepository, hermesMetrics,
                kafkaNamesMapper, blacklistZookeeperNotifyingCache).provide();
    }

    @Bean
    public AvroEnforcer messageContentTypeEnforcer() {
        return new MessageContentTypeEnforcer();
    }

    @Bean
    public TopicMessageValidatorListFactory topicMessageValidatorListFactory() {
        return new TopicMessageValidatorListFactory(new ArrayList<>());
    }

    @Bean
    public MessageFactory messageFactory(MessageValidators validators,
                                         AvroEnforcer enforcer,
                                         SchemaRepository schemaRepository,
                                         HeadersPropagator headersPropagator,
                                         MessageContentWrapper messageContentWrapper,
                                         Clock clock,
                                         ConfigFactory configFactory) {
        return new MessageFactory(validators, enforcer, schemaRepository, headersPropagator, messageContentWrapper,
                clock, configFactory);
    }

    @Bean
    public BackupMessagesLoader backupMessagesLoader(BrokerMessageProducer brokerMessageProducer,
                                                     BrokerListeners brokerListeners,
                                                     TopicsCache topicsCache,
                                                     Trackers trackers,
                                                     ConfigFactory config) {
        return new BackupMessagesLoader(brokerMessageProducer, brokerListeners, topicsCache, trackers, config);
    }

    @Bean
    public PersistentBufferExtension persistentBufferExtension(ConfigFactory configFactory,
                                                               Clock clock,
                                                               BrokerListeners listeners,
                                                               HooksHandler hooksHandler,
                                                               BackupMessagesLoader backupMessagesLoader,
                                                               HermesMetrics hermesMetrics) {
        return new PersistentBufferExtension(configFactory, clock, listeners, hooksHandler, backupMessagesLoader,
                hermesMetrics);
    }

    @Bean
    public MessagePreviewPersister messagePreviewPersister(MessagePreviewLog messagePreviewLog,
                                                           MessagePreviewRepository repository,
                                                           ConfigFactory configFactory) {
        return new MessagePreviewPersister(messagePreviewLog, repository, configFactory);
    }

    @Bean
    public MessagePreviewLog messagePreviewLog(MessagePreviewFactory messagePreviewFactory,
                                               ConfigFactory configFactory) {
        return new MessagePreviewLog(messagePreviewFactory, configFactory);
    }

    @Bean
    public MessagePreviewFactory messagePreviewFactory(ConfigFactory configFactory) {
        return new MessagePreviewFactory(configFactory);
    }

    @Bean
    public KafkaHeaderFactory kafkaHeaderFactory(ConfigFactory configFactory) {
        return new KafkaHeaderFactory(configFactory);
    }

    @Bean
    public BlacklistZookeeperNotifyingCache blacklistZookeeperNotifyingCache(@Named(CuratorType.HERMES) CuratorFramework curator,
                                                                             ZookeeperPaths zookeeperPaths) {
        return new BlacklistZookeeperNotifyingCacheFactory(curator, zookeeperPaths).provide();
    }

    @Bean(destroyMethod = "close")
    public ReadinessRepository readinessRepository(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                                   ZookeeperPaths paths,
                                                   ObjectMapper mapper) {
        return new ReadinessRepositoryFactory(zookeeper, paths, mapper).provide();
    }
}
