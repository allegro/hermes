package pl.allegro.tech.hermes.consumers.di.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.avro.Schema;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.clock.ClockFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.di.CuratorType;
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
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.SchemaOnlineChecksRateLimiter;
import pl.allegro.tech.hermes.common.message.wrapper.SchemaOnlineChecksWaitingRateLimiter;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterStorage;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.common.schema.AvroCompiledSchemaRepositoryFactory;
import pl.allegro.tech.hermes.common.schema.RawSchemaClientFactory;
import pl.allegro.tech.hermes.common.schema.SchemaRepositoryFactory;
import pl.allegro.tech.hermes.common.schema.SchemaRepositoryInstanceResolverFactory;
import pl.allegro.tech.hermes.common.schema.SchemaVersionsRepositoryFactory;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
//TODO: add scopes to each bean?
//TODO: @Inject
public class CommonConfiguration {

    @Bean
    public SubscriptionRepository subscriptionRepository(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                                         ZookeeperPaths paths,
                                                         ObjectMapper mapper,
                                                         TopicRepository topicRepository) {
        return new SubscriptionRepositoryFactory(zookeeper, paths, mapper, topicRepository).provide();
    }

    @Bean
    public OAuthProviderRepository oAuthProviderRepository(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                                           ZookeeperPaths paths,
                                                           ObjectMapper mapper) {
        return new OAuthProviderRepositoryFactory(zookeeper, paths, mapper).provide();
    }

    @Bean
    public TopicRepository topicRepository(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                           ZookeeperPaths paths,
                                           ObjectMapper mapper,
                                           GroupRepository groupRepository) {
        return new TopicRepositoryFactory(zookeeper, paths, mapper, groupRepository).provide();
    }

    @Bean
    public GroupRepository groupRepository(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                           ZookeeperPaths paths,
                                           ObjectMapper mapper) {
        return new GroupRepositoryFactory(zookeeper, paths, mapper).provide();
    }

    @Bean (destroyMethod = "close")
    @Named(CuratorType.HERMES)
    public CuratorFramework hermesCurator(ConfigFactory configFactory,
                                          CuratorClientFactory curatorClientFactory) {
        return new HermesCuratorClientFactory(configFactory, curatorClientFactory).provide();
    }

    @Bean
    public CuratorClientFactory curatorClientFactory(ConfigFactory configFactory) {
        return new CuratorClientFactory(configFactory);
    }

//    //TODO: remove?
//    @Bean
//    @ConditionalOnMissingBean(name = { "messageFilters", "messageFiltersSource"})
//    public MessageFilterSource messageFilterSource() {
//        return new MessageFilters(Collections.emptyList(), Collections.emptyList());
//    }

    @Bean
    public FilterChainFactory filterChainFactory(MessageFilterSource filters) {
        return new FilterChainFactory(filters);
    }

    @Bean
    public InternalNotificationsBus zookeeperInternalNotificationBus(ObjectMapper objectMapper,
                                                                     ModelAwareZookeeperNotifyingCache modelNotifyingCache) {
        return new ZookeeperInternalNotificationBus(objectMapper, modelNotifyingCache);
    }

    @Bean(destroyMethod = "stop")
    public ModelAwareZookeeperNotifyingCache modelAwareZookeeperNotifyingCache(@Named(CuratorType.HERMES) CuratorFramework curator,
                                                                               ConfigFactory config) {
        return new ModelAwareZookeeperNotifyingCacheFactory(curator, config).provide();
    }

    @Bean
    public UndeliveredMessageLog undeliveredMessageLog(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
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
                                                   @Named(CuratorType.HERMES) CuratorFramework client,
                                                   ObjectMapper objectMapper,
                                                   Clock clock) {
        return new ZookeeperAdminCache(zookeeperPaths, client, objectMapper, clock);
    }

    @Bean
    public ObjectMapper objectMapper(ConfigFactory configFactory) {
        return new ObjectMapperFactory(configFactory).provide();
    }


    @Bean
    public MessageContentWrapper messageContentWrapper(JsonMessageContentWrapper jsonMessageContentWrapper,
                                                       AvroMessageContentWrapper avroMessageContentWrapper,
                                                       AvroMessageSchemaIdAwareContentWrapper schemaIdAwareContentWrapper,
                                                       AvroMessageHeaderSchemaVersionContentWrapper headerSchemaVersionContentWrapper,
                                                       AvroMessageHeaderSchemaIdContentWrapper headerSchemaIdContentWrapper,
                                                       AvroMessageAnySchemaVersionContentWrapper anySchemaVersionContentWrapper,
                                                       AvroMessageSchemaVersionTruncationContentWrapper schemaVersionTruncationContentWrapper) {
        return new MessageContentWrapper(jsonMessageContentWrapper, avroMessageContentWrapper, schemaIdAwareContentWrapper,
                headerSchemaVersionContentWrapper, headerSchemaIdContentWrapper, anySchemaVersionContentWrapper,
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
    public AvroMessageAnySchemaVersionContentWrapper anySchemaVersionContentWrapper(SchemaRepository schemaRepository,
                                                                                    SchemaOnlineChecksRateLimiter schemaOnlineChecksRateLimiter,
                                                                                    AvroMessageContentWrapper avroMessageContentWrapper,
                                                                                    DeserializationMetrics deserializationMetrics) {
        return new AvroMessageAnySchemaVersionContentWrapper(schemaRepository, schemaOnlineChecksRateLimiter,
                avroMessageContentWrapper, deserializationMetrics);
    }

    @Bean
    public SchemaOnlineChecksRateLimiter schemaOnlineChecksWaitingRateLimiter(ConfigFactory configFactory) {
        return new SchemaOnlineChecksWaitingRateLimiter(configFactory);
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
//    @ConditionalOnMissingBean
    public KafkaNamesMapper prodKafkaNamesMapper(ConfigFactory configFactory) {
        return new KafkaNamesMapperFactory(configFactory).provide();
    }

    //TODO - use property instead?
    @Bean
    @Named("moduleName")
    public String moduleName() {
        return "consumer";
    }

    @Bean
    public ConfigFactory prodConfigFactory(ApplicationArguments applicationArguments) {
        List<String> values = Arrays.stream(Configs.values()).map(Configs::getName).collect(Collectors.toList());
        Map<String, Object> map = applicationArguments.getOptionNames().stream()
                .filter(values::contains)
                .collect(Collectors.toMap(Function.identity(), applicationArguments::getOptionValues));
        AbstractConfiguration abstractConfiguration = new MapConfiguration(map);
        return new ConfigFactoryCreator(abstractConfiguration).provide();
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
    public WorkloadConstraintsRepository workloadConstraintsRepository(@Named(CuratorType.HERMES) CuratorFramework
                                                                               curator,
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
    public SharedCounter sharedCounter(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                       ConfigFactory config) {
        return new SharedCounterFactory(zookeeper, config).provide();
    }

    @Bean
    public InstanceIdResolver instanceIdResolver() {
        return new InetAddressInstanceIdResolver();
    }

    @Bean
    public SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicatorFactory(
            @Named(CuratorType.HERMES) CuratorFramework zookeeper,
            ZookeeperPaths paths,
            SubscriptionRepository subscriptionRepository) {
        return new SubscriptionOffsetChangeIndicatorFactory(zookeeper, paths, subscriptionRepository).provide();
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

}
