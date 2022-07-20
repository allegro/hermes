package pl.allegro.tech.hermes.frontend.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.clock.ClockFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.di.factories.ConfigFactoryCreator;
import pl.allegro.tech.hermes.common.di.factories.CuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.HermesCuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.MetricRegistryFactory;
import pl.allegro.tech.hermes.common.di.factories.ModelAwareZookeeperNotifyingCacheFactory;
import pl.allegro.tech.hermes.common.di.factories.ObjectMapperFactory;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapperFactory;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.message.undelivered.ZookeeperUndeliveredMessageLog;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageHeaderSchemaIdContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageHeaderSchemaVersionContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageSchemaIdAwareContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageSchemaVersionTruncationContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.DeserializationMetrics;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterStorage;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.common.util.InetAddressInstanceIdResolver;
import pl.allegro.tech.hermes.common.util.InstanceIdResolver;
import pl.allegro.tech.hermes.domain.filtering.MessageFilter;
import pl.allegro.tech.hermes.domain.filtering.MessageFilterSource;
import pl.allegro.tech.hermes.domain.filtering.MessageFilters;
import pl.allegro.tech.hermes.domain.filtering.SubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.domain.filtering.avro.AvroPathSubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.domain.filtering.header.HeaderSubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.domain.filtering.json.JsonPathSubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperGroupRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperMessagePreviewRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperOAuthProviderRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperWorkloadConstraintsRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.ModelAwareZookeeperNotifyingCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter;
import pl.allegro.tech.hermes.infrastructure.zookeeper.notifications.ZookeeperInternalNotificationBus;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.schema.SchemaRepository;

import javax.inject.Named;
import java.time.Clock;
import java.util.List;

@Configuration
public class CommonConfiguration {

    @Bean
    public SubscriptionRepository subscriptionRepository(CuratorFramework zookeeper,
                                                         ZookeeperPaths paths,
                                                         ObjectMapper mapper,
                                                         TopicRepository topicRepository) {
        return new ZookeeperSubscriptionRepository(zookeeper, mapper, paths, topicRepository);
    }

    @Bean
    public OAuthProviderRepository oAuthProviderRepository(CuratorFramework zookeeper,
                                                           ZookeeperPaths paths,
                                                           ObjectMapper mapper) {
        return new ZookeeperOAuthProviderRepository(zookeeper, mapper, paths);
    }

    @Bean
    public TopicRepository topicRepository(CuratorFramework zookeeper,
                                           ZookeeperPaths paths,
                                           ObjectMapper mapper,
                                           GroupRepository groupRepository) {
        return new ZookeeperTopicRepository(zookeeper, mapper, paths, groupRepository);
    }

    @Bean
    public GroupRepository groupRepository(CuratorFramework zookeeper,
                                           ZookeeperPaths paths,
                                           ObjectMapper mapper) {
        return new ZookeeperGroupRepository(zookeeper, mapper, paths);
    }

    @Bean(destroyMethod = "close")
    public CuratorFramework hermesCurator(ConfigFactory configFactory,
                                          CuratorClientFactory curatorClientFactory) {
        return new HermesCuratorClientFactory(configFactory, curatorClientFactory).provide();
    }

    @Bean
    public CuratorClientFactory curatorClientFactory(ConfigFactory configFactory) {
        return new CuratorClientFactory(configFactory);
    }

    @Bean
    public FilterChainFactory filterChainFactory(MessageFilterSource filters) {
        return new FilterChainFactory(filters);
    }

    @Bean
    public InternalNotificationsBus zookeeperInternalNotificationBus(ObjectMapper objectMapper,
                                                                     ModelAwareZookeeperNotifyingCache modelNotifyingCache) {
        return new ZookeeperInternalNotificationBus(objectMapper, modelNotifyingCache);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public ModelAwareZookeeperNotifyingCache modelAwareZookeeperNotifyingCache(CuratorFramework curator,
                                                                               ConfigFactory config) {
        return new ModelAwareZookeeperNotifyingCacheFactory(curator, config).provide();
    }

    @Bean
    public UndeliveredMessageLog undeliveredMessageLog(CuratorFramework zookeeper,
                                                       ZookeeperPaths paths,
                                                       ObjectMapper mapper) {
        return new ZookeeperUndeliveredMessageLog(zookeeper, paths, mapper);
    }

    @Bean
    public InstrumentedExecutorServiceFactory instrumentedExecutorServiceFactory(HermesMetrics hermesMetrics) {
        return new InstrumentedExecutorServiceFactory(hermesMetrics);
    }

    @Bean
    public ZookeeperAdminCache zookeeperAdminCache(ZookeeperPaths zookeeperPaths,
                                                   CuratorFramework client,
                                                   ObjectMapper objectMapper,
                                                   Clock clock) {
        return new ZookeeperAdminCache(zookeeperPaths, client, objectMapper, clock);
    }

    @Bean
    public ObjectMapper objectMapper(ConfigFactory configFactory) {
        return new ObjectMapperFactory(configFactory).provide();
    }

    @Bean
    public CompositeMessageContentWrapper messageContentWrapper(JsonMessageContentWrapper jsonMessageContentWrapper,
                                                                AvroMessageContentWrapper avroMessageContentWrapper,
                                                                AvroMessageSchemaIdAwareContentWrapper schemaIdAwareContentWrapper,
                                                                AvroMessageHeaderSchemaVersionContentWrapper headerSchemaVersionContentWrapper,
                                                                AvroMessageHeaderSchemaIdContentWrapper headerSchemaIdContentWrapper,
                                                                AvroMessageSchemaVersionTruncationContentWrapper schemaVersionTruncationContentWrapper) {
        return new CompositeMessageContentWrapper(jsonMessageContentWrapper, avroMessageContentWrapper, schemaIdAwareContentWrapper,
                headerSchemaVersionContentWrapper, headerSchemaIdContentWrapper,
                schemaVersionTruncationContentWrapper);
    }

    @Bean
    public JsonMessageContentWrapper jsonMessageContentWrapper(ConfigFactory config,
                                                               ObjectMapper mapper) {
        return new JsonMessageContentWrapper(config, mapper);
    }

    @Bean
    public AvroMessageContentWrapper avroMessageContentWrapper(Clock clock) {
        return new AvroMessageContentWrapper(clock);
    }

    @Bean
    public AvroMessageSchemaVersionTruncationContentWrapper avroMessageSchemaVersionTruncationContentWrapper(SchemaRepository schemaRepository,
                                                                                                             AvroMessageContentWrapper avroMessageContentWrapper,
                                                                                                             DeserializationMetrics deserializationMetrics,
                                                                                                             ConfigFactory configFactory) {
        return new AvroMessageSchemaVersionTruncationContentWrapper(schemaRepository, avroMessageContentWrapper,
                deserializationMetrics, configFactory);
    }

    @Bean
    public DeserializationMetrics deserializationMetrics(MetricRegistry metricRegistry) {
        return new DeserializationMetrics(metricRegistry);
    }

    @Bean
    public AvroMessageHeaderSchemaIdContentWrapper avroMessageHeaderSchemaIdContentWrapper(SchemaRepository schemaRepository,
                                                                                           AvroMessageContentWrapper avroMessageContentWrapper,
                                                                                           DeserializationMetrics deserializationMetrics,
                                                                                           ConfigFactory configFactory) {
        return new AvroMessageHeaderSchemaIdContentWrapper(schemaRepository, avroMessageContentWrapper,
                deserializationMetrics, configFactory);
    }

    @Bean
    public AvroMessageHeaderSchemaVersionContentWrapper avroMessageHeaderSchemaVersionContentWrapper(SchemaRepository schemaRepository,
                                                                                                     AvroMessageContentWrapper avroMessageContentWrapper,
                                                                                                     DeserializationMetrics deserializationMetrics) {
        return new AvroMessageHeaderSchemaVersionContentWrapper(schemaRepository, avroMessageContentWrapper,
                deserializationMetrics);
    }

    @Bean
    public AvroMessageSchemaIdAwareContentWrapper avroMessageSchemaIdAwareContentWrapper(SchemaRepository schemaRepository,
                                                                                         AvroMessageContentWrapper avroMessageContentWrapper,
                                                                                         DeserializationMetrics deserializationMetrics) {
        return new AvroMessageSchemaIdAwareContentWrapper(schemaRepository, avroMessageContentWrapper,
                deserializationMetrics);
    }

    @Bean
    public KafkaNamesMapper prodKafkaNamesMapper(ConfigFactory configFactory) {
        return new KafkaNamesMapperFactory(configFactory).provide();
    }

    @Bean
    public ConfigFactory prodConfigFactory() {
        return new ConfigFactoryCreator().provide();
    }

    @Bean
    public Clock clock() {
        return new ClockFactory().provide();
    }

    @Bean
    public ZookeeperPaths zookeeperPaths(ConfigFactory configFactory) {
        return new ZookeeperPaths(configFactory.getStringProperty(Configs.ZOOKEEPER_ROOT));
    }

    @Bean
    public WorkloadConstraintsRepository workloadConstraintsRepository(CuratorFramework curator,
                                                                       ObjectMapper mapper,
                                                                       ZookeeperPaths paths) {
        return new ZookeeperWorkloadConstraintsRepository(curator, mapper, paths);
    }

    @Bean
    public HermesMetrics hermesMetrics(MetricRegistry metricRegistry,
                                       PathsCompiler pathCompiler) {
        return new HermesMetrics(metricRegistry, pathCompiler);
    }

    @Bean
    public MetricRegistry metricRegistry(ConfigFactory configFactory,
                                         CounterStorage counterStorage,
                                         InstanceIdResolver instanceIdResolver,
                                         @Named("moduleName") String moduleName) {
        return new MetricRegistryFactory(configFactory, counterStorage, instanceIdResolver, moduleName)
                .provide();
    }

    @Bean
    public PathsCompiler pathsCompiler(InstanceIdResolver instanceIdResolver) {
        return new PathsCompiler(instanceIdResolver.resolve());
    }

    @Bean
    public CounterStorage zookeeperCounterStorage(SharedCounter sharedCounter,
                                                  SubscriptionRepository subscriptionRepository,
                                                  PathsCompiler pathsCompiler,
                                                  ConfigFactory configFactory) {
        return new ZookeeperCounterStorage(sharedCounter, subscriptionRepository, pathsCompiler, configFactory);
    }

    @Bean
    public SharedCounter sharedCounter(CuratorFramework zookeeper,
                                       ConfigFactory config) {
        return new SharedCounter(zookeeper,
                config.getIntProperty(Configs.METRICS_COUNTER_EXPIRE_AFTER_ACCESS),
                config.getIntProperty(Configs.ZOOKEEPER_BASE_SLEEP_TIME),
                config.getIntProperty(Configs.ZOOKEEPER_MAX_RETRIES)
        );    }

    @Bean
    public InstanceIdResolver instanceIdResolver() {
        return new InetAddressInstanceIdResolver();
    }

    @Bean
    public SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicatorFactory(
            CuratorFramework zookeeper,
            ZookeeperPaths paths,
            SubscriptionRepository subscriptionRepository) {
        return new ZookeeperSubscriptionOffsetChangeIndicator(zookeeper, paths, subscriptionRepository);
    }

    @Bean
    public MessageFilters messageFilters(List<MessageFilter> globalFilters,
                                         List<SubscriptionMessageFilterCompiler> subscriptionMessageFilterCompilers) {
        return new MessageFilters(globalFilters, subscriptionMessageFilterCompilers);
    }

    @Bean
    public SubscriptionMessageFilterCompiler jsonPathSubscriptionMessageFilterCompiler() {
        return new JsonPathSubscriptionMessageFilterCompiler();
    }

    @Bean
    public SubscriptionMessageFilterCompiler avroPathSubscriptionMessageFilterCompiler() {
        return new AvroPathSubscriptionMessageFilterCompiler();
    }

    @Bean
    public SubscriptionMessageFilterCompiler headerSubscriptionMessageFilterCompiler() {
        return new HeaderSubscriptionMessageFilterCompiler();
    }

    @Bean
    public MessagePreviewRepository zookeeperMessagePreviewRepository(CuratorFramework zookeeper,
                                                                      ObjectMapper mapper,
                                                                      ZookeeperPaths paths) {
        return new ZookeeperMessagePreviewRepository(zookeeper, mapper, paths);
    }
}
