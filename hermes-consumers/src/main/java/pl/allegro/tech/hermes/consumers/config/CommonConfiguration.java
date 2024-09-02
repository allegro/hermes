package pl.allegro.tech.hermes.consumers.config;

import static io.micrometer.core.instrument.Clock.SYSTEM;
import static java.util.Collections.emptyList;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import jakarta.inject.Named;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.clock.ClockFactory;
import pl.allegro.tech.hermes.common.concurrent.DefaultExecutorServiceFactory;
import pl.allegro.tech.hermes.common.concurrent.ExecutorServiceFactory;
import pl.allegro.tech.hermes.common.di.factories.CuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.HermesCuratorClientFactory;
import pl.allegro.tech.hermes.common.di.factories.MicrometerRegistryParameters;
import pl.allegro.tech.hermes.common.di.factories.ModelAwareZookeeperNotifyingCacheFactory;
import pl.allegro.tech.hermes.common.di.factories.ObjectMapperFactory;
import pl.allegro.tech.hermes.common.di.factories.PrometheusMeterRegistryFactory;
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
import pl.allegro.tech.hermes.common.message.wrapper.JsonMessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
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
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;
import pl.allegro.tech.hermes.infrastructure.dc.DcNameSource;
import pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider;
import pl.allegro.tech.hermes.infrastructure.dc.EnvironmentVariableDatacenterNameProvider;
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

@Configuration
@EnableConfigurationProperties({
  MetricsProperties.class,
  MicrometerRegistryProperties.class,
  PrometheusProperties.class,
  SchemaProperties.class,
  ZookeeperClustersProperties.class,
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
  public SubscriptionRepository subscriptionRepository(
      CuratorFramework zookeeper,
      ZookeeperPaths paths,
      ObjectMapper mapper,
      TopicRepository topicRepository) {
    return new ZookeeperSubscriptionRepository(zookeeper, mapper, paths, topicRepository);
  }

  @Bean
  public OAuthProviderRepository oAuthProviderRepository(
      CuratorFramework zookeeper, ZookeeperPaths paths, ObjectMapper mapper) {
    return new ZookeeperOAuthProviderRepository(zookeeper, mapper, paths);
  }

  @Bean
  public TopicRepository topicRepository(
      CuratorFramework zookeeper,
      ZookeeperPaths paths,
      ObjectMapper mapper,
      GroupRepository groupRepository) {
    return new ZookeeperTopicRepository(zookeeper, mapper, paths, groupRepository);
  }

  @Bean
  public GroupRepository groupRepository(
      CuratorFramework zookeeper, ZookeeperPaths paths, ObjectMapper mapper) {
    return new ZookeeperGroupRepository(zookeeper, mapper, paths);
  }

  @Bean(destroyMethod = "close")
  public CuratorFramework hermesCurator(
      ZookeeperClustersProperties zookeeperClustersProperties,
      CuratorClientFactory curatorClientFactory,
      DatacenterNameProvider datacenterNameProvider) {
    ZookeeperProperties zookeeperProperties =
        zookeeperClustersProperties.toZookeeperProperties(datacenterNameProvider);
    return new HermesCuratorClientFactory(zookeeperProperties, curatorClientFactory).provide();
  }

  @Bean
  public CuratorClientFactory curatorClientFactory(
      ZookeeperClustersProperties zookeeperClustersProperties,
      DatacenterNameProvider datacenterNameProvider) {
    ZookeeperProperties zookeeperProperties =
        zookeeperClustersProperties.toZookeeperProperties(datacenterNameProvider);
    return new CuratorClientFactory(zookeeperProperties);
  }

  @Bean
  public InternalNotificationsBus zookeeperInternalNotificationBus(
      ObjectMapper objectMapper, ModelAwareZookeeperNotifyingCache modelNotifyingCache) {
    return new ZookeeperInternalNotificationBus(objectMapper, modelNotifyingCache);
  }

  @Bean(destroyMethod = "stop")
  public ModelAwareZookeeperNotifyingCache modelAwareZookeeperNotifyingCache(
      CuratorFramework curator,
      MetricsFacade metricsFacade,
      ZookeeperClustersProperties zookeeperClustersProperties,
      DatacenterNameProvider datacenterNameProvider) {
    ZookeeperProperties zookeeperProperties =
        zookeeperClustersProperties.toZookeeperProperties(datacenterNameProvider);
    return new ModelAwareZookeeperNotifyingCacheFactory(curator, metricsFacade, zookeeperProperties)
        .provide();
  }

  @Bean
  public UndeliveredMessageLog undeliveredMessageLog(
      CuratorFramework zookeeper,
      ZookeeperPaths paths,
      ObjectMapper mapper,
      MetricsFacade metricsFacade) {
    return new ZookeeperUndeliveredMessageLog(zookeeper, paths, mapper, metricsFacade);
  }

  @Bean
  public InstrumentedExecutorServiceFactory instrumentedExecutorServiceFactory(
      MetricsFacade metricsFacade) {
    return new InstrumentedExecutorServiceFactory(metricsFacade);
  }

  @Bean
  public ZookeeperAdminCache zookeeperAdminCache(
      ZookeeperPaths zookeeperPaths,
      CuratorFramework client,
      ObjectMapper objectMapper,
      Clock clock) {
    return new ZookeeperAdminCache(zookeeperPaths, client, objectMapper, clock);
  }

  @Bean
  public ObjectMapper objectMapper(SchemaProperties schemaProperties) {
    return new ObjectMapperFactory(
            schemaProperties.isIdSerializationEnabled(),
            /* fallbackToRemoteDatacenter is frontend specific property, we so don't expose consumer side property for it */
            false)
        .provide();
  }

  @Bean
  public CompositeMessageContentWrapper messageContentWrapper(
      ObjectMapper mapper,
      Clock clock,
      SchemaRepository schemaRepository,
      MetricsFacade metricsFacade,
      SchemaProperties schemaProperties,
      ContentRootProperties contentRootProperties) {
    AvroMessageContentWrapper avroMessageContentWrapper = new AvroMessageContentWrapper(clock);

    return new CompositeMessageContentWrapper(
        new JsonMessageContentWrapper(
            contentRootProperties.getMessage(), contentRootProperties.getMetadata(), mapper),
        avroMessageContentWrapper,
        new AvroMessageSchemaIdAwareContentWrapper(
            schemaRepository, avroMessageContentWrapper, metricsFacade),
        new AvroMessageHeaderSchemaVersionContentWrapper(
            schemaRepository, avroMessageContentWrapper, metricsFacade),
        new AvroMessageHeaderSchemaIdContentWrapper(
            schemaRepository,
            avroMessageContentWrapper,
            metricsFacade,
            schemaProperties.isIdHeaderEnabled()),
        new AvroMessageSchemaVersionTruncationContentWrapper(
            schemaRepository,
            avroMessageContentWrapper,
            metricsFacade,
            schemaProperties.isVersionTruncationEnabled()));
  }

  @Bean
  public KafkaNamesMapper prodKafkaNamesMapper(KafkaClustersProperties kafkaClustersProperties) {
    return new NamespaceKafkaNamesMapper(
        kafkaClustersProperties.getNamespace(), kafkaClustersProperties.getNamespaceSeparator());
  }

  @Bean
  public Clock clock() {
    return new ClockFactory().provide();
  }

  @Bean
  public ZookeeperPaths zookeeperPaths(
      ZookeeperClustersProperties zookeeperClustersProperties,
      DatacenterNameProvider datacenterNameProvider) {
    ZookeeperProperties zookeeperProperties =
        zookeeperClustersProperties.toZookeeperProperties(datacenterNameProvider);
    return new ZookeeperPaths(zookeeperProperties.getRoot());
  }

  @Bean
  public WorkloadConstraintsRepository workloadConstraintsRepository(
      CuratorFramework curator, ObjectMapper mapper, ZookeeperPaths paths) {
    return new ZookeeperWorkloadConstraintsRepository(curator, mapper, paths);
  }

  @Bean
  public MetricsFacade metricsFacade(MeterRegistry meterRegistry) {
    return new MetricsFacade(meterRegistry);
  }

  @Bean
  PrometheusConfig prometheusConfig(PrometheusProperties properties) {
    return new PrometheusConfigAdapter(properties);
  }

  @Bean
  public PrometheusMeterRegistry micrometerRegistry(
      MicrometerRegistryParameters micrometerRegistryParameters,
      PrometheusConfig prometheusConfig,
      CounterStorage counterStorage) {
    return new PrometheusMeterRegistryFactory(
            micrometerRegistryParameters, prometheusConfig, counterStorage, "hermes-consumers")
        .provide();
  }

  @Bean
  @Primary
  public MeterRegistry compositeMeterRegistry(List<MeterRegistry> registries) {
    return new CompositeMeterRegistry(SYSTEM, registries);
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
  public CounterStorage zookeeperCounterStorage(
      SharedCounter sharedCounter,
      SubscriptionRepository subscriptionRepository,
      PathsCompiler pathsCompiler,
      ZookeeperClustersProperties zookeeperClustersProperties,
      DatacenterNameProvider datacenterNameProvider) {
    ZookeeperProperties zookeeperProperties =
        zookeeperClustersProperties.toZookeeperProperties(datacenterNameProvider);
    return new ZookeeperCounterStorage(
        sharedCounter, subscriptionRepository, pathsCompiler, zookeeperProperties.getRoot());
  }

  @Bean
  public SharedCounter sharedCounter(
      CuratorFramework zookeeper,
      ZookeeperClustersProperties zookeeperClustersProperties,
      MetricsProperties metricsProperties,
      DatacenterNameProvider datacenterNameProvider) {
    ZookeeperProperties zookeeperProperties =
        zookeeperClustersProperties.toZookeeperProperties(datacenterNameProvider);
    return new SharedCounter(
        zookeeper,
        metricsProperties.getCounterExpireAfterAccess(),
        zookeeperProperties.getBaseSleepTime(),
        zookeeperProperties.getMaxRetries());
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
  FilterChainFactory filterChainFactory() {
    List<SubscriptionMessageFilterCompiler> subscriptionFilterCompilers =
        Arrays.asList(
            new AvroPathSubscriptionMessageFilterCompiler(),
            new JsonPathSubscriptionMessageFilterCompiler(),
            new HeaderSubscriptionMessageFilterCompiler());
    MessageFilters messageFilters = new MessageFilters(emptyList(), subscriptionFilterCompilers);
    return new FilterChainFactory(messageFilters);
  }

  @Bean
  public ExecutorServiceFactory executorServiceFactory() {
    return new DefaultExecutorServiceFactory();
  }
}
