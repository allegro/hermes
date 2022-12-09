package pl.allegro.tech.hermes.frontend.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.clock.ClockFactory;
import pl.allegro.tech.hermes.common.di.factories.CuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.HermesCuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.MetricRegistryFactory;
import pl.allegro.tech.hermes.common.di.factories.ModelAwareZookeeperNotifyingCacheFactory;
import pl.allegro.tech.hermes.common.di.factories.ObjectMapperFactory;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.message.undelivered.ZookeeperUndeliveredMessageLog;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageHeaderSchemaIdContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageHeaderSchemaVersionContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageSchemaIdAwareContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageSchemaVersionTruncationContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.DeserializationMetrics;
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterStorage;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.common.metric.executor.ThreadPoolMetrics;
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
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;
import pl.allegro.tech.hermes.infrastructure.dc.DcNameSource;
import pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider;
import pl.allegro.tech.hermes.infrastructure.dc.EnvironmentVariableDatacenterNameProvider;
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

import java.time.Clock;
import java.util.List;
import javax.inject.Named;

@Configuration
@EnableConfigurationProperties({
        MetricsProperties.class,
        GraphiteProperties.class,
        SchemaProperties.class,
        ZookeeperClustersProperties.class,
        KafkaClustersProperties.class,
        ContentRootProperties.class,
        DatacenterNameProperties.class
})
public class CommonConfiguration {

    @Bean
    public DatacenterNameProvider dcNameProvider(DatacenterNameProperties datacenterNameProperties) {
        if (datacenterNameProperties.getSource() == DcNameSource.ENV) {
            return new EnvironmentVariableDatacenterNameProvider(datacenterNameProperties.getEnv());
        } else {
            return new DefaultDatacenterNameProvider();
        }
    }

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
    public CuratorFramework hermesCurator(ZookeeperClustersProperties zookeeperClustersProperties,
                                          CuratorClientFactory curatorClientFactory,
                                          DatacenterNameProvider datacenterNameProvider) {
        ZookeeperProperties zookeeperProperties = zookeeperClustersProperties.toZookeeperProperties(datacenterNameProvider);
        return new HermesCuratorClientFactory(zookeeperProperties, curatorClientFactory).provide();
    }

    @Bean
    public CuratorClientFactory curatorClientFactory(ZookeeperClustersProperties zookeeperClustersProperties,
                                                     DatacenterNameProvider datacenterNameProvider) {
        ZookeeperProperties zookeeperProperties = zookeeperClustersProperties.toZookeeperProperties(datacenterNameProvider);
        return new CuratorClientFactory(zookeeperProperties);
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
                                                                               ZookeeperClustersProperties zookeeperClustersProperties,
                                                                               DatacenterNameProvider datacenterNameProvider) {
        ZookeeperProperties zookeeperProperties = zookeeperClustersProperties.toZookeeperProperties(datacenterNameProvider);
        return new ModelAwareZookeeperNotifyingCacheFactory(curator, zookeeperProperties).provide();
    }

    @Bean
    public UndeliveredMessageLog undeliveredMessageLog(CuratorFramework zookeeper,
                                                       ZookeeperPaths paths,
                                                       ObjectMapper mapper,
                                                       HermesMetrics hermesMetrics) {
        return new ZookeeperUndeliveredMessageLog(zookeeper, paths, mapper, hermesMetrics);
    }

    @Bean
    public ThreadPoolMetrics threadPoolMetrics(HermesMetrics hermesMetrics) {
        return new ThreadPoolMetrics(hermesMetrics);
    }

    @Bean
    public InstrumentedExecutorServiceFactory instrumentedExecutorServiceFactory(ThreadPoolMetrics threadPoolMetrics) {
        return new InstrumentedExecutorServiceFactory(threadPoolMetrics);
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
    public CompositeMessageContentWrapper messageContentWrapper(
            JsonMessageContentWrapper jsonMessageContentWrapper,
            AvroMessageContentWrapper avroMessageContentWrapper,
            AvroMessageSchemaIdAwareContentWrapper schemaIdAwareContentWrapper,
            AvroMessageHeaderSchemaVersionContentWrapper headerSchemaVersionContentWrapper,
            AvroMessageHeaderSchemaIdContentWrapper headerSchemaIdContentWrapper,
            AvroMessageSchemaVersionTruncationContentWrapper schemaVersionTruncationContentWrapper) {
        return new CompositeMessageContentWrapper(
                jsonMessageContentWrapper,
                avroMessageContentWrapper,
                schemaIdAwareContentWrapper,
                headerSchemaVersionContentWrapper,
                headerSchemaIdContentWrapper,
                schemaVersionTruncationContentWrapper);
    }

    @Bean
    public JsonMessageContentWrapper jsonMessageContentWrapper(ContentRootProperties contentRootProperties,
                                                               ObjectMapper mapper) {
        return new JsonMessageContentWrapper(contentRootProperties.getMessage(), contentRootProperties.getMetadata(), mapper);
    }

    @Bean
    public AvroMessageContentWrapper avroMessageContentWrapper(Clock clock) {
        return new AvroMessageContentWrapper(clock);
    }

    @Bean
    public AvroMessageSchemaVersionTruncationContentWrapper avroMessageSchemaVersionTruncationContentWrapper(
            SchemaRepository schemaRepository,
            AvroMessageContentWrapper avroMessageContentWrapper,
            DeserializationMetrics deserializationMetrics,
            SchemaProperties schemaProperties) {
        return new AvroMessageSchemaVersionTruncationContentWrapper(schemaRepository, avroMessageContentWrapper,
                deserializationMetrics, schemaProperties.isVersionTruncationEnabled());
    }

    @Bean
    public DeserializationMetrics deserializationMetrics(MetricRegistry metricRegistry) {
        return new DeserializationMetrics(metricRegistry);
    }

    @Bean
    public AvroMessageHeaderSchemaIdContentWrapper avroMessageHeaderSchemaIdContentWrapper(
            SchemaRepository schemaRepository,
            AvroMessageContentWrapper avroMessageContentWrapper,
            DeserializationMetrics deserializationMetrics,
            SchemaProperties schemaProperties) {
        return new AvroMessageHeaderSchemaIdContentWrapper(schemaRepository, avroMessageContentWrapper,
                deserializationMetrics, schemaProperties.isIdHeaderEnabled());
    }

    @Bean
    public AvroMessageHeaderSchemaVersionContentWrapper avroMessageHeaderSchemaVersionContentWrapper(
            SchemaRepository schemaRepository,
            AvroMessageContentWrapper avroMessageContentWrapper,
            DeserializationMetrics deserializationMetrics) {
        return new AvroMessageHeaderSchemaVersionContentWrapper(schemaRepository, avroMessageContentWrapper,
                deserializationMetrics);
    }

    @Bean
    public AvroMessageSchemaIdAwareContentWrapper avroMessageSchemaIdAwareContentWrapper(
            SchemaRepository schemaRepository,
            AvroMessageContentWrapper avroMessageContentWrapper,
            DeserializationMetrics deserializationMetrics) {
        return new AvroMessageSchemaIdAwareContentWrapper(schemaRepository, avroMessageContentWrapper,
                deserializationMetrics);
    }

    @Bean
    public KafkaNamesMapper prodKafkaNamesMapper(KafkaClustersProperties kafkaClustersProperties) {
        return new NamespaceKafkaNamesMapper(kafkaClustersProperties.getNamespace(), kafkaClustersProperties.getNamespaceSeparator());
    }

    @Bean
    public Clock clock() {
        return new ClockFactory().provide();
    }

    @Bean
    public ZookeeperPaths zookeeperPaths(ZookeeperClustersProperties zookeeperClustersProperties,
                                         DatacenterNameProvider datacenterNameProvider) {
        ZookeeperProperties zookeeperProperties = zookeeperClustersProperties.toZookeeperProperties(datacenterNameProvider);
        return new ZookeeperPaths(zookeeperProperties.getRoot());
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
        return new MetricRegistryFactory(metricsProperties, graphiteProperties, counterStorage, instanceIdResolver, moduleName)
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
                                                  ZookeeperClustersProperties zookeeperClustersProperties,
                                                  DatacenterNameProvider datacenterNameProvider) {
        ZookeeperProperties zookeeperProperties = zookeeperClustersProperties.toZookeeperProperties(datacenterNameProvider);
        return new ZookeeperCounterStorage(sharedCounter, subscriptionRepository, pathsCompiler, zookeeperProperties.getRoot());
    }

    @Bean
    public SharedCounter sharedCounter(CuratorFramework zookeeper,
                                       ZookeeperClustersProperties zookeeperClustersProperties,
                                       MetricsProperties metricsProperties,
                                       DatacenterNameProvider datacenterNameProvider) {
        ZookeeperProperties zookeeperProperties = zookeeperClustersProperties.toZookeeperProperties(datacenterNameProvider);
        return new SharedCounter(zookeeper,
                metricsProperties.getCounterExpireAfterAccess(),
                zookeeperProperties.getBaseSleepTime(),
                zookeeperProperties.getMaxRetries()
        );
    }

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
