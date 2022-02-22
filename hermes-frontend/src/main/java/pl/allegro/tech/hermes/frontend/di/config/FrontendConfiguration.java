package pl.allegro.tech.hermes.frontend.di.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.frontend.blacklist.BlacklistZookeeperNotifyingCache;
import pl.allegro.tech.hermes.frontend.buffer.BackupMessagesLoader;
import pl.allegro.tech.hermes.frontend.cache.topic.NotificationBasedTopicsCache;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.di.PersistentBufferExtension;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaBrokerMessageProducer;
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
import pl.allegro.tech.hermes.frontend.server.auth.AuthenticationConfiguration;
import pl.allegro.tech.hermes.frontend.services.HealthCheckService;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import pl.allegro.tech.hermes.frontend.validator.TopicMessageValidator;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperDatacenterReadinessRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.tracker.frontend.LogRepository;
import pl.allegro.tech.hermes.tracker.frontend.NoOperationPublishingTracker;
import pl.allegro.tech.hermes.tracker.frontend.PublishingMessageTracker;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import javax.inject.Named;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Configuration
public class FrontendConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    @DependsOn("beforeStartupHooksHandler")
    @Order(LifecycleOrder.SERVER_STARTUP)
    public HermesServer hermesServer(ConfigFactory configFactory,
                                     HermesMetrics hermesMetrics,
                                     HttpHandler publishingHandler,
                                     HealthCheckService healthCheckService,
                                     ReadinessChecker readinessChecker,
                                     MessagePreviewPersister messagePreviewPersister,
                                     ThroughputLimiter throughputLimiter,
                                     TopicMetadataLoadingJob topicMetadataLoadingJob,
                                     SslContextFactoryProvider sslContextFactoryProvider,
                                     ConfigurableApplicationContext applicationContext) {
        return new HermesServer(configFactory, hermesMetrics, publishingHandler, healthCheckService, readinessChecker,
                messagePreviewPersister, throughputLimiter, topicMetadataLoadingJob, sslContextFactoryProvider, applicationContext);
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

    @Bean//TODO: use as list element or add init method + Order?
    @Conditional(TopicMetadataLoadingStartupHookCondition.class)//TODO: eventually change to ConditionalOnProperty
    //    @Bean(initMethod = "run")
//    @Order(LifecycleOrder.BEFORE_STARTUP)
//    @DependsOn("configFactory")//TODO - do we need it or not?
    public TopicMetadataLoadingStartupHook topicMetadataLoadingStartupHook(TopicMetadataLoadingRunner topicMetadataLoadingRunner) {
        return new TopicMetadataLoadingStartupHook(topicMetadataLoadingRunner);
    }

    @Bean//TODO: use as list element or add init method + Order?
//    @Bean(initMethod = "run")
//    @Order(LifecycleOrder.BEFORE_STARTUP)
    @Conditional(TopicSchemaLoadingStartupHookCondition.class)//TODO: eventually change to ConditionalOnProperty
    public TopicSchemaLoadingStartupHook topicSchemaLoadingStartupHook(TopicsCache topicsCache,
                                                                       SchemaRepository schemaRepository,
                                                                       ConfigFactory config) {
        return new TopicSchemaLoadingStartupHook(topicsCache, schemaRepository, config);
    }

    @Bean(initMethod = "runHooks")
    //TODO: remove?
    @Order(LifecycleOrder.BEFORE_STARTUP)
    public BeforeStartupHooksHandler beforeStartupHooksHandler(List<BeforeStartupHook> hooks) {
        return new BeforeStartupHooksHandler(hooks);
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
                                   ThroughputLimiter throughputLimiter, Optional<AuthenticationConfiguration> authConfig) {
        return new HandlersChainFactory(topicsCache, messageErrorProcessor, messageEndProcessor, configFactory, messageFactory,
                brokerMessageProducer, messagePreviewLog, throughputLimiter, authConfig.orElse(null)).provide();
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
        return new KafkaBrokerMessageProducer(producers, kafkaTopicMetadataFetcher, hermesMetrics, kafkaHeaderFactory,
                configFactory);
//        return new KafkaBrokerMessageProducerFactory(producers, kafkaTopicMetadataFetcher, hermesMetrics,
//                kafkaHeaderFactory, configFactory).provide();
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

    @Bean(initMethod = "start")
    public TopicsCache notificationBasedTopicsCache(InternalNotificationsBus internalNotificationsBus,
                                                    GroupRepository groupRepository,
                                                    TopicRepository topicRepository,
                                                    HermesMetrics hermesMetrics,
                                                    KafkaNamesMapper kafkaNamesMapper,
                                                    BlacklistZookeeperNotifyingCache blacklistZookeeperNotifyingCache) {

        return new NotificationBasedTopicsCache(internalNotificationsBus, blacklistZookeeperNotifyingCache,
                groupRepository, topicRepository, hermesMetrics, kafkaNamesMapper);

//        cache.start();
//        return cache;

//        return new TopicsCacheFactory(internalNotificationsBus, groupRepository, topicRepository, hermesMetrics,
//                kafkaNamesMapper, blacklistZookeeperNotifyingCache).provide();
    }

    @Bean
    public AvroEnforcer messageContentTypeEnforcer() {
        return new MessageContentTypeEnforcer();
    }

//    @Bean
//    public List<TopicMessageValidator> topicMessageValidators() { //TODO - Spring will autowire any bean to a list
//        return new TopicMessageValidatorListFactory(topicMessageValidators);
//    }

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

    @Bean(initMethod = "extend")
    @Order(LifecycleOrder.PERSISTENT_BUFFER_STARTUP)//TODO - do we need it? does it really matters?
    public PersistentBufferExtension persistentBufferExtension(ConfigFactory configFactory,
                                                               Clock clock,
                                                               BrokerListeners listeners,
//                                                               HooksHandler hooksHandler,
                                                               BackupMessagesLoader backupMessagesLoader,
                                                               HermesMetrics hermesMetrics) {
        return new PersistentBufferExtension(configFactory, clock, listeners, backupMessagesLoader,
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
        BlacklistZookeeperNotifyingCache cache = new BlacklistZookeeperNotifyingCache(curator, zookeeperPaths);
        try {
            cache.start(); //TODO - change to init method?
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start Zookeeper Topic Blacklist cache", e);
        }
        return cache;

//        return new BlacklistZookeeperNotifyingCacheFactory(curator, zookeeperPaths).provide();
    }

    @Bean(destroyMethod = "close")
    public ReadinessRepository zookeeperDatacenterReadinessRepository(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                                                      ZookeeperPaths paths,
                                                                      ObjectMapper mapper) {
        return new ZookeeperDatacenterReadinessRepository(zookeeper, mapper, paths);
//        return new ReadinessRepositoryFactory(zookeeper, paths, mapper).provide();
    }

    @Bean
    public SslContextFactoryProvider sslContextFactoryProvider(Optional<SslContextFactory> sslContextFactory,
                                                               ConfigFactory configFactory) {
        return new SslContextFactoryProvider(sslContextFactory.orElse(null), configFactory);
    }

    @Bean(initMethod = "startup")
    @Order(LifecycleOrder.AFTER_STARTUP)
    public HealthCheckService healthCheckService() {
        return new HealthCheckService();
    }

    @Bean
    public BrokerListeners defaultBrokerListeners() {
        return new BrokerListeners();
    }

    @Bean
    public Trackers trackers(List<LogRepository> repositories) {
        return new Trackers(repositories);
    }

    @Bean
    @Named("moduleName")
    public String moduleName() {
        return "producer";
    }
}
