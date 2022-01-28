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
import pl.allegro.tech.hermes.consumers.consumer.interpolation.MessageBodyInterpolator;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.UriInterpolator;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.hooks.SpringHooksHandler;
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
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.resolver.SchemaRepositoryInstanceResolver;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SIGNAL_PROCESSING_QUEUE_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_REPOSITORY_HTTP_CONNECT_TIMEOUT_MS;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_REPOSITORY_HTTP_READ_TIMEOUT_MS;

@Configuration
//TODO: add scopes to each bean
//TODO: ogarnąć wszystkie field i method injections
//TODO: ogarnac metody dispose
//TODO: split - commons, consumers etc.
public class SpringConfiguration {

    @Bean
    public ApplicationContext applicationContext() {
        return new GenericApplicationContext();
    }

    @Bean
    public SubscriptionRepositoryFactory subscriptionRepositoryFactory(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                                                       ZookeeperPaths paths,
                                                                       ObjectMapper mapper,
                                                                       TopicRepository topicRepository) {
        return new SubscriptionRepositoryFactory(zookeeper, paths, mapper, topicRepository);
    }

    //TODO: make as 1 bean with Factory?
    @Bean
    public SubscriptionRepository subscriptionRepository(SubscriptionRepositoryFactory subscriptionRepositoryFactory) {
        return subscriptionRepositoryFactory.provide();
    }


    //TODO: make as 1 bean with Factory?
    @Bean
    public OAuthProviderRepository oAuthProviderRepository(OAuthProviderRepositoryFactory oAuthProviderRepositoryFactory) {
        return oAuthProviderRepositoryFactory.provide();
    }

    @Bean
    public OAuthProviderRepositoryFactory oAuthProviderRepositoryFactory(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                                                         ZookeeperPaths paths,
                                                                         ObjectMapper mapper) {
        return new OAuthProviderRepositoryFactory(zookeeper, paths, mapper);
    }

    //TODO: make as 1 bean with Factory?
    @Bean
    public TopicRepository topicRepository(TopicRepositoryFactory topicRepositoryFactory) {
        return topicRepositoryFactory.provide();
    }

    @Bean
    public TopicRepositoryFactory topicRepositoryFactory(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                                         ZookeeperPaths paths,
                                                         ObjectMapper mapper,
                                                         GroupRepository groupRepository) {
        return new TopicRepositoryFactory(zookeeper, paths, mapper, groupRepository);
    }

    @Bean
    public GroupRepository groupRepository(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                           ZookeeperPaths paths,
                                           ObjectMapper mapper) {
        return new GroupRepositoryFactory(zookeeper, paths, mapper).provide();
    }

    //TODO: make as 1 bean with Factory?
    @Bean
    @Named(CuratorType.HERMES)//TODO - remove
    public CuratorFramework hermesCurator(ConfigFactory configFactory,
                                          CuratorClientFactory curatorClientFactory) {
        return new HermesCuratorClientFactory(configFactory, curatorClientFactory).provide();
    }

    @Bean
    public CuratorClientFactory curatorClientFactory(ConfigFactory configFactory) {
        return new CuratorClientFactory(configFactory);
    }

    //TODO??
    @Bean
    public Trackers trackers() {
        return new Trackers(new ArrayList<>());
    }


//    //TODO: wywalić?
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

    @Bean
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
    public SchemaRepository schemaRepository(SchemaVersionsRepository schemaVersionsRepository,
                                             CompiledSchemaRepository<Schema> compiledAvroSchemaRepository) {
        return new SchemaRepositoryFactory(schemaVersionsRepository, compiledAvroSchemaRepository).provide();
    }

    @Bean
    public CompiledSchemaRepository<Schema> avroCompiledSchemaRepository(RawSchemaClient rawSchemaClient,
                                                                         ConfigFactory configFactory) {
        return new AvroCompiledSchemaRepositoryFactory(rawSchemaClient, configFactory).provide();
    }

    @Bean
    public RawSchemaClient rawSchemaClient(ConfigFactory configFactory,
                                           HermesMetrics hermesMetrics,
                                           ObjectMapper objectMapper,
                                           SchemaRepositoryInstanceResolver resolver) {
        return new RawSchemaClientFactory(configFactory, hermesMetrics, objectMapper, resolver).provide();
    }

    @Bean
    public SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver(ConfigFactory configFactory,
                                                                             Client client) { //TODO
        return new SchemaRepositoryInstanceResolverFactory(configFactory, client).provide();
    }

    @Bean(name = "schemaRepositoryClient")
    public Client schemaRepositoryClient(ObjectMapper mapper, ConfigFactory configFactory) {
        ClientConfig config = new ClientConfig()
                .property(ClientProperties.READ_TIMEOUT, configFactory.getIntProperty(SCHEMA_REPOSITORY_HTTP_READ_TIMEOUT_MS))
                .property(ClientProperties.CONNECT_TIMEOUT, configFactory.getIntProperty(SCHEMA_REPOSITORY_HTTP_CONNECT_TIMEOUT_MS))
                .register(new JacksonJsonProvider(mapper));

        return ClientBuilder.newClient(config);
    }

    @Bean
    public SchemaVersionsRepository schemaVersionsRepositoryFactory(RawSchemaClient rawSchemaClient,
                                                                    ConfigFactory configFactory,
                                                                    InternalNotificationsBus notificationsBus,
                                                                    CompiledSchemaRepository compiledSchemaRepository) {//TODO ??
        return new SchemaVersionsRepositoryFactory(rawSchemaClient, configFactory, notificationsBus, compiledSchemaRepository)
                .provide();
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
    public UriInterpolator messageBodyInterpolator() {
        return new MessageBodyInterpolator();
    }

    @Bean
//    @ConditionalOnMissingBean
    public KafkaNamesMapper kafkaNamesMapper(ConfigFactory configFactory) {
        return new KafkaNamesMapperFactory(configFactory).provide();
    }

    //TODO - use property instead?
    @Bean
    @Named("moduleName")//TODO - remove
    public String moduleName() {
        return "consumer";
    }

    @Bean
    public ConfigFactory configFactory(ApplicationArguments applicationArguments) {
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
//    @ConditionalOnMissingBean
    public SpringHooksHandler springHooksHandler() {
        return new SpringHooksHandler();
    }

    @Bean
    public MessageSenderProviders messageSenderProviders(ProtocolMessageSenderProvider defaultHttpMessageSenderProvider,
                                                               ProtocolMessageSenderProvider defaultHttpsMessageSenderProvider,
                                                               ProtocolMessageSenderProvider defaultJmsMessageSenderProvider) {
        return new MessageSenderProviders(
                defaultHttpMessageSenderProvider,
                defaultHttpsMessageSenderProvider,
                defaultJmsMessageSenderProvider);
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
