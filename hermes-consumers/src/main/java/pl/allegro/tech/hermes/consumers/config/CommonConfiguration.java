package pl.allegro.tech.hermes.consumers.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.clock.ClockFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.di.factories.ConfigFactoryCreator;
import pl.allegro.tech.hermes.common.di.factories.CuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.GroupRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.HermesCuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.MetricRegistryFactory;
import pl.allegro.tech.hermes.common.di.factories.ModelAwareZookeeperNotifyingCacheFactory;
import pl.allegro.tech.hermes.common.di.factories.OAuthProviderRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.ObjectMapperFactory;
import pl.allegro.tech.hermes.common.di.factories.PathsCompilerFactory;
import pl.allegro.tech.hermes.common.di.factories.SharedCounterFactory;
import pl.allegro.tech.hermes.common.di.factories.SubscriptionOffsetChangeIndicatorFactory;
import pl.allegro.tech.hermes.common.di.factories.SubscriptionRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.TopicRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.UndeliveredMessageLogFactory;
import pl.allegro.tech.hermes.common.di.factories.WorkloadConstraintsRepositoryFactory;
import pl.allegro.tech.hermes.common.di.factories.ZookeeperPathsFactory;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapperFactory;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
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
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
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
public class CommonConfiguration {

    @Bean
    public SubscriptionRepository subscriptionRepository(CuratorFramework zookeeper,
                                                         ZookeeperPaths paths,
                                                         ObjectMapper mapper,
                                                         TopicRepository topicRepository) {
        return new SubscriptionRepositoryFactory(zookeeper, paths, mapper, topicRepository).provide();
    }

    @Bean
    public OAuthProviderRepository oAuthProviderRepository(CuratorFramework zookeeper,
                                                           ZookeeperPaths paths,
                                                           ObjectMapper mapper) {
        return new OAuthProviderRepositoryFactory(zookeeper, paths, mapper).provide();
    }

    @Bean
    public TopicRepository topicRepository(CuratorFramework zookeeper,
                                           ZookeeperPaths paths,
                                           ObjectMapper mapper,
                                           GroupRepository groupRepository) {
        return new TopicRepositoryFactory(zookeeper, paths, mapper, groupRepository).provide();
    }

    @Bean
    public GroupRepository groupRepository(CuratorFramework zookeeper, ZookeeperPaths paths, ObjectMapper mapper) {
        return new GroupRepositoryFactory(zookeeper, paths, mapper).provide();
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
        return new UndeliveredMessageLogFactory(zookeeper, paths, mapper).provide();
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
    public CompositeMessageContentWrapper messageContentWrapper(ConfigFactory config,
                                                                ObjectMapper mapper,
                                                                Clock clock,
                                                                SchemaRepository schemaRepository,
                                                                DeserializationMetrics deserializationMetrics,
                                                                ConfigFactory configFactory,
                                                                SchemaOnlineChecksRateLimiter schemaOnlineChecksRateLimiter) {
        AvroMessageContentWrapper avroMessageContentWrapper = new AvroMessageContentWrapper(clock);

        return new CompositeMessageContentWrapper(
                new JsonMessageContentWrapper(config, mapper),
                avroMessageContentWrapper,
                new AvroMessageSchemaIdAwareContentWrapper(schemaRepository, avroMessageContentWrapper,
                        deserializationMetrics),
                new AvroMessageHeaderSchemaVersionContentWrapper(schemaRepository, avroMessageContentWrapper,
                        deserializationMetrics),
                new AvroMessageHeaderSchemaIdContentWrapper(schemaRepository, avroMessageContentWrapper,
                        deserializationMetrics, configFactory),
                new AvroMessageAnySchemaVersionContentWrapper(schemaRepository, schemaOnlineChecksRateLimiter,
                        avroMessageContentWrapper, deserializationMetrics),
                new AvroMessageSchemaVersionTruncationContentWrapper(schemaRepository, avroMessageContentWrapper,
                        deserializationMetrics, configFactory)
        );
    }

    @Bean
    public DeserializationMetrics deserializationMetrics(MetricRegistry metricRegistry) {
        return new DeserializationMetrics(metricRegistry);
    }

    @Bean
    public SchemaOnlineChecksRateLimiter schemaOnlineChecksWaitingRateLimiter(ConfigFactory configFactory) {
        return new SchemaOnlineChecksWaitingRateLimiter(configFactory);
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
        return new ZookeeperPathsFactory(configFactory).provide();
    }

    @Bean
    public WorkloadConstraintsRepository workloadConstraintsRepository(CuratorFramework curator,
                                                                       ObjectMapper mapper,
                                                                       ZookeeperPaths paths) {
        return new WorkloadConstraintsRepositoryFactory(curator, mapper, paths).provide();
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
    @Named("moduleName")
    public String moduleName() {
        return "consumer";
    }

    @Bean
    public PathsCompiler pathsCompiler(InstanceIdResolver instanceIdResolver) {
        return new PathsCompilerFactory(instanceIdResolver).provide();
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
        return new SharedCounterFactory(zookeeper, config).provide();
    }

    @Bean
    public InstanceIdResolver instanceIdResolver() {
        return new InetAddressInstanceIdResolver();
    }

    @Bean
    public SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicatorFactory(CuratorFramework zookeeper,
                                                                                      ZookeeperPaths paths,
                                                                                      SubscriptionRepository subscriptionRepository) {
        return new SubscriptionOffsetChangeIndicatorFactory(zookeeper, paths, subscriptionRepository).provide();
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
