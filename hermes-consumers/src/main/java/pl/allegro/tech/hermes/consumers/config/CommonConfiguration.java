package pl.allegro.tech.hermes.consumers.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageAnySchemaVersionContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageHeaderSchemaIdContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageHeaderSchemaVersionContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageSchemaIdAwareContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageSchemaVersionTruncationContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.DeserializationMetrics;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.SchemaOnlineChecksRateLimiter;
import pl.allegro.tech.hermes.common.message.wrapper.SchemaOnlineChecksWaitingRateLimiter;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterStorage;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.common.util.InetAddressInstanceIdResolver;
import pl.allegro.tech.hermes.common.util.InstanceIdResolver;
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
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperGroupRepository;
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
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

@Configuration
@EnableConfigurationProperties({
        MetricsProperties.class,
        GraphiteProperties.class,
        SchemaProperties.class
})
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
    public GroupRepository groupRepository(CuratorFramework zookeeper, ZookeeperPaths paths, ObjectMapper mapper) {
        return new ZookeeperGroupRepository(zookeeper, mapper, paths);
    }

    @Bean(destroyMethod = "close")
    public CuratorFramework hermesCurator(ConfigFactory configFactory, CuratorClientFactory curatorClientFactory) {
        return new HermesCuratorClientFactory(configFactory, curatorClientFactory).provide();
    }

    @Bean
    public CuratorClientFactory curatorClientFactory(ConfigFactory configFactory) {
        return new CuratorClientFactory(configFactory);
    }

    @Bean
    public InternalNotificationsBus zookeeperInternalNotificationBus(ObjectMapper objectMapper,
                                                                     ModelAwareZookeeperNotifyingCache modelNotifyingCache) {
        return new ZookeeperInternalNotificationBus(objectMapper, modelNotifyingCache);
    }

    @Bean(destroyMethod = "stop")
    public ModelAwareZookeeperNotifyingCache modelAwareZookeeperNotifyingCache(CuratorFramework curator, ConfigFactory config) {
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
    public ObjectMapper objectMapper(SchemaProperties schemaProperties) {
        return new ObjectMapperFactory(schemaProperties.isIdSerializationEnabled()).provide();
    }


    @Bean
    public CompositeMessageContentWrapper messageContentWrapper(ConfigFactory config,
                                                                ObjectMapper mapper,
                                                                Clock clock,
                                                                SchemaRepository schemaRepository,
                                                                DeserializationMetrics deserializationMetrics,
                                                                SchemaOnlineChecksRateLimiter schemaOnlineChecksRateLimiter,
                                                                SchemaProperties schemaProperties) {
        AvroMessageContentWrapper avroMessageContentWrapper = new AvroMessageContentWrapper(clock);

        return new CompositeMessageContentWrapper(
                new JsonMessageContentWrapper(config, mapper),
                avroMessageContentWrapper,
                new AvroMessageSchemaIdAwareContentWrapper(schemaRepository, avroMessageContentWrapper,
                        deserializationMetrics),
                new AvroMessageHeaderSchemaVersionContentWrapper(schemaRepository, avroMessageContentWrapper,
                        deserializationMetrics),
                new AvroMessageHeaderSchemaIdContentWrapper(schemaRepository, avroMessageContentWrapper,
                        deserializationMetrics, schemaProperties.isIdHeaderEnabled()),
                new AvroMessageAnySchemaVersionContentWrapper(schemaRepository, schemaOnlineChecksRateLimiter,
                        avroMessageContentWrapper, deserializationMetrics),
                new AvroMessageSchemaVersionTruncationContentWrapper(schemaRepository, avroMessageContentWrapper,
                        deserializationMetrics, schemaProperties.isVersionTruncationEnabled())
        );
    }

    @Bean
    public DeserializationMetrics deserializationMetrics(MetricRegistry metricRegistry) {
        return new DeserializationMetrics(metricRegistry);
    }

    @Bean
    public SchemaOnlineChecksRateLimiter schemaOnlineChecksWaitingRateLimiter(SchemaProperties schemaProperties) {
        return new SchemaOnlineChecksWaitingRateLimiter(
                schemaProperties.getRepository().getOnlineCheckPermitsPerSecond(),
                schemaProperties.getRepository().getOnlineCheckAcquireWaitMs()
        );
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
    public MetricRegistry metricRegistry(MetricsProperties metricsProperties,
                                         GraphiteProperties graphiteProperties,
                                         CounterStorage counterStorage,
                                         InstanceIdResolver instanceIdResolver,
                                         @Named("moduleName") String moduleName) {
        return new MetricRegistryFactory(metricsProperties.toMetricRegistryParameters(), graphiteProperties.toGraphiteParameters(), counterStorage, instanceIdResolver, moduleName)
                .provide();
    }

    @Bean
    @Named("moduleName")
    public String moduleName() {
        return "consumer";
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
                                       ConfigFactory config,
                                       MetricsProperties metricsProperties) {
        return new SharedCounter(zookeeper,
                metricsProperties.getCounterExpireAfterAccess(),
                config.getIntProperty(Configs.ZOOKEEPER_BASE_SLEEP_TIME),
                config.getIntProperty(Configs.ZOOKEEPER_MAX_RETRIES)
        );    }

    @Bean
    public InstanceIdResolver instanceIdResolver() {
        return new InetAddressInstanceIdResolver();
    }

    @Bean
    public SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicatorFactory(CuratorFramework zookeeper,
                                                                                      ZookeeperPaths paths,
                                                                                      SubscriptionRepository subscriptionRepository) {
        return new ZookeeperSubscriptionOffsetChangeIndicator(zookeeper, paths, subscriptionRepository);
    }

    @Bean
    FilterChainFactory filterChainFactory() {
        List<SubscriptionMessageFilterCompiler> subscriptionFilterCompilers = Arrays.asList(
                new AvroPathSubscriptionMessageFilterCompiler(),
                new JsonPathSubscriptionMessageFilterCompiler(),
                new HeaderSubscriptionMessageFilterCompiler()
        );
        MessageFilters messageFilters = new MessageFilters(emptyList(), subscriptionFilterCompilers);
        return new FilterChainFactory(messageFilters);
    }

}
