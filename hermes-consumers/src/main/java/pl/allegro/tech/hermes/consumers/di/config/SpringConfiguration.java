package pl.allegro.tech.hermes.consumers.di.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.avro.Schema;
import org.apache.curator.framework.CuratorFramework;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.GenericApplicationContext;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.clock.ClockFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
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
import pl.allegro.tech.hermes.consumers.consumer.ConsumerAuthorizationHandler;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerMessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.batch.ByteBufferMessageBatchFactoryProvider;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchFactory;
import pl.allegro.tech.hermes.consumers.consumer.converter.AvroToJsonMessageConverter;
import pl.allegro.tech.hermes.consumers.consumer.converter.DefaultMessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.converter.NoOperationMessageConverter;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.MessageBodyInterpolator;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.UriInterpolator;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthAccessTokens;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthAccessTokensLoader;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthConsumerAuthorizationHandler;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthProvidersNotifyingCache;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthProvidersNotifyingCacheFactory;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthSubscriptionAccessTokens;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthSubscriptionHandlerFactory;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthTokenRequestRateLimiterFactory;
import pl.allegro.tech.hermes.consumers.consumer.oauth.client.OAuthClient;
import pl.allegro.tech.hermes.consumers.consumer.oauth.client.OAuthHttpClient;
import pl.allegro.tech.hermes.consumers.consumer.oauth.client.OAuthHttpClientFactory;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimitSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculatorFactory;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRatePathSerializer;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateProviderFactory;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateRegistry;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateRegistryFactory;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.BasicMessageContentReaderFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaHeaderExtractor;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaMessageReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.MessageContentReaderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.HttpMessageBatchSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.DefaultHttpMetadataAppender;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.DefaultHttpRequestFactoryProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.DefaultSendingResultHandlers;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.EmptyHttpHeadersProvidersFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.Http2ClientFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.Http2ClientHolder;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientsFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientsWorkloadReporter;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpHeadersProvidersFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpRequestFactoryProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.JettyHttpMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.SendingResultHandlers;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.SslContextFactoryProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.HttpAuthorizationProviderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.jms.JmsHornetQMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.jms.JmsMetadataAppender;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.InterpolatingEndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeoutFactory;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;
import pl.allegro.tech.hermes.consumers.health.ConsumerHttpServer;
import pl.allegro.tech.hermes.consumers.health.ConsumerMonitor;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistryFactory;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionCacheFactory;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIdProvider;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIdProviderFactory;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIdsCacheFactory;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerFactory;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersExecutorService;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.NonblockingConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.monitor.ConsumersRuntimeMonitor;
import pl.allegro.tech.hermes.consumers.supervisor.monitor.ConsumersRuntimeMonitorFactory;
import pl.allegro.tech.hermes.consumers.supervisor.process.Retransmitter;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ClusterAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ClusterAssignmentCacheFactory;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentCacheFactory;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentRegistryFactory;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorController;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorControllerFactory;
import pl.allegro.tech.hermes.domain.filtering.MessageFilterSource;
import pl.allegro.tech.hermes.domain.filtering.MessageFilters;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
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
import javax.jms.Message;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;

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
    public ConsumerAuthorizationHandler oAuthConsumerAuthorizationHandler(OAuthSubscriptionHandlerFactory handlerFactory,
                                                                          ConfigFactory configFactory,
                                                                          OAuthProvidersNotifyingCache oAuthProvidersCache) {
        return new OAuthConsumerAuthorizationHandler(handlerFactory, configFactory, oAuthProvidersCache);
    }

    @Bean
    public OAuthSubscriptionHandlerFactory oAuthSubscriptionHandlerFactory(SubscriptionRepository subscriptionRepository,
                                                                           OAuthAccessTokens accessTokens,
                                                                           OAuthTokenRequestRateLimiterFactory rateLimiterLoader) {
        return new OAuthSubscriptionHandlerFactory(subscriptionRepository, accessTokens, rateLimiterLoader);
    }

    @Bean
    public OAuthTokenRequestRateLimiterFactory oAuthTokenRequestRateLimiterFactory(OAuthProviderRepository oAuthProviderRepository,
                                                                                   ConfigFactory configFactory) {
        return new OAuthTokenRequestRateLimiterFactory(oAuthProviderRepository, configFactory);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)//TODO - bindFactory, raczej singleton
    public OAuthProvidersNotifyingCache oAuthProvidersNotifyingCache(@Named(CuratorType.HERMES) CuratorFramework curator,
                                                                     ZookeeperPaths paths,
                                                                     ObjectMapper objectMapper) {
        return new OAuthProvidersNotifyingCacheFactory(curator, paths, objectMapper).provide();
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

    @Bean
    public OAuthAccessTokens oAuthSubscriptionAccessTokens(OAuthAccessTokensLoader tokenLoader,
                                                           ConfigFactory configFactory) {
        return new OAuthSubscriptionAccessTokens(tokenLoader, configFactory);
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

    @Bean
    public OAuthAccessTokensLoader oAuthAccessTokensLoader(SubscriptionRepository subscriptionRepository,
                                                           OAuthProviderRepository oAuthProviderRepository,
                                                           OAuthClient oAuthClient,
                                                           HermesMetrics metrics) {
        return new OAuthAccessTokensLoader(subscriptionRepository, oAuthProviderRepository, oAuthClient, metrics);
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

    @Bean
    public OAuthClient oAuthHttpClient(@Named("oauth-http-client") HttpClient httpClient,
                                       ObjectMapper objectMapper) {
        return new OAuthHttpClient(httpClient, objectMapper);
    }

    //TODO??
    @Bean
    public Trackers trackers() {
        return new Trackers(new ArrayList<>());
    }

    @Bean
    public MessageFilterSource messageFilterSource() {
        return new MessageFilters(Collections.emptyList(), Collections.emptyList());
    }

    @Bean
    public MetadataAppender<Message> jmsMetadataAppender() {
        return new JmsMetadataAppender();
    }

    @Bean
    public MetadataAppender<Request> defaultHttpMetadataAppender() {
        return new DefaultHttpMetadataAppender();
    }

    @Bean
    public FutureAsyncTimeout<MessageSendingResult> futureAsyncTimeoutFactory(ConfigFactory configFactory,
                                                                              InstrumentedExecutorServiceFactory executorFactory) {
        return new FutureAsyncTimeoutFactory(configFactory, executorFactory).provide();
    }

    @Bean
    public FilterChainFactory filterChainFactory(MessageFilterSource filters) {
        return new FilterChainFactory(filters);
    }

    //TODO: merge into 1 bean with factory
    @Bean(name = "http-1-client")
    public HttpClient http1Client(HttpClientsFactory httpClientsFactory) {
        return new HttpClientFactory(httpClientsFactory).provide();
    }

    @Bean(name = "oauth-http-client")
    public HttpClient oauthHttpClient(HttpClientsFactory httpClientsFactory) {
        return new OAuthHttpClientFactory(httpClientsFactory).provide();
    }

    @Bean
    public HttpClientsFactory httpClientsFactory(ConfigFactory configFactory,
                                                 InstrumentedExecutorServiceFactory executorFactory,
                                                 SslContextFactoryProvider sslContextFactoryProvider) {
        return new HttpClientsFactory(configFactory, executorFactory, sslContextFactoryProvider);
    }

    @Bean
    public MessageBatchSenderFactory httpMessageBatchSenderFactory(ConfigFactory configFactory,
                                                                   SendingResultHandlers resultHandlers) {
        return new HttpMessageBatchSenderFactory(configFactory, resultHandlers);
    }

    @Bean
    public MessageBatchFactory messageBatchFactory(HermesMetrics hermesMetrics,
                                                   Clock clock,
                                                   ConfigFactory configFactory) {
        return new ByteBufferMessageBatchFactoryProvider(hermesMetrics, clock, configFactory).provide();
    }

    @Bean
    public UndeliveredMessageLogPersister undeliveredMessageLogPersister(UndeliveredMessageLog undeliveredMessageLog,
                                                                         ConfigFactory configFactory) {
        return new UndeliveredMessageLogPersister(undeliveredMessageLog, configFactory);
    }

    @Bean
    public MaxRateRegistry maxRateRegistry(ConfigFactory configFactory,
                                           CuratorFramework curator,
                                           ObjectMapper objectMapper,
                                           ZookeeperPaths zookeeperPaths,
                                           MaxRatePathSerializer pathSerializer,
                                           SubscriptionsCache subscriptionCache,
                                           SubscriptionIds subscriptionIds,
                                           ConsumerAssignmentCache assignmentCache,
                                           ClusterAssignmentCache clusterAssignmentCache) {
        return new MaxRateRegistryFactory(configFactory, curator, objectMapper, zookeeperPaths,
                pathSerializer, subscriptionCache, subscriptionIds, assignmentCache, clusterAssignmentCache)
                .provide();
    }

    @Bean
    public MaxRateProviderFactory maxRateProviderFactory(ConfigFactory configFactory,
                                                         MaxRateRegistry maxRateRegistry,
                                                         MaxRateSupervisor maxRateSupervisor,
                                                         HermesMetrics metrics) {
        return new MaxRateProviderFactory(configFactory, maxRateRegistry, maxRateSupervisor, metrics);
    }

    @Bean
    public MaxRateSupervisor maxRateSupervisor(ConfigFactory configFactory,
                                               ClusterAssignmentCache clusterAssignmentCache,
                                               MaxRateRegistry maxRateRegistry,
                                               ConsumerNodesRegistry consumerNodesRegistry,
                                               SubscriptionsCache subscriptionsCache,
                                               ZookeeperPaths zookeeperPaths,
                                               HermesMetrics metrics,
                                               Clock clock) {
        return new MaxRateSupervisor(configFactory, clusterAssignmentCache, maxRateRegistry,
                consumerNodesRegistry, subscriptionsCache, zookeeperPaths, metrics, clock);
    }

    @Bean
    public MaxRatePathSerializer maxRatePathSerializer() {
        return new MaxRatePathSerializer();
    }

    @Bean
    public Http2ClientHolder http2ClientHolder(HttpClientsFactory httpClientsFactory,
                                               ConfigFactory configFactory) {
        return new Http2ClientFactory(httpClientsFactory, configFactory).provide();
    }

    @Bean
    public ConsumersRuntimeMonitor consumersRuntimeMonitor(ConsumersSupervisor consumerSupervisor,
                                                           SupervisorController workloadSupervisor,
                                                           HermesMetrics hermesMetrics,
                                                           SubscriptionsCache subscriptionsCache,
                                                           ConfigFactory configFactory) {
        return new ConsumersRuntimeMonitorFactory(consumerSupervisor, workloadSupervisor,
                hermesMetrics, subscriptionsCache, configFactory)
                .provide();
    }

    @Bean
    public SupervisorController supervisorController(InternalNotificationsBus notificationsBus,
                                                     ConsumerNodesRegistry consumerNodesRegistry,
                                                     ConsumerAssignmentRegistry assignmentRegistry,
                                                     ConsumerAssignmentCache consumerAssignmentCache,
                                                     ClusterAssignmentCache clusterAssignmentCache,
                                                     SubscriptionsCache subscriptionsCache,
                                                     ConsumersSupervisor supervisor,
                                                     ZookeeperAdminCache adminCache,
                                                     HermesMetrics metrics,
                                                     ConfigFactory configs,
                                                     WorkloadConstraintsRepository workloadConstraintsRepository) {
        return new SupervisorControllerFactory(notificationsBus, consumerNodesRegistry, assignmentRegistry,
                consumerAssignmentCache, clusterAssignmentCache, subscriptionsCache, supervisor, adminCache,
                metrics, configs, workloadConstraintsRepository)
                .provide();
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
    public ConsumerAssignmentRegistry consumerAssignmentRegistry(CuratorFramework curator,
                                                                 ConfigFactory configFactory,
                                                                 ZookeeperPaths zookeeperPaths,
                                                                 ConsumerAssignmentCache consumerAssignmentCache,
                                                                 SubscriptionIds subscriptionIds) {
        return new ConsumerAssignmentRegistryFactory(curator, configFactory, zookeeperPaths,
                consumerAssignmentCache, subscriptionIds)
                .provide();
    }

    @Bean
    public UndeliveredMessageLog undeliveredMessageLog(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                                       ZookeeperPaths paths,
                                                       ObjectMapper mapper) {
        return new UndeliveredMessageLogFactory(zookeeper, paths, mapper).provide();
    }

    @Bean
    public HttpClientsWorkloadReporter httpClientsWorkloadReporter(HermesMetrics metrics,
                                                                   @Named("http-1-client") HttpClient httpClient,
                                                                   Http2ClientHolder http2ClientHolder,
                                                                   ConfigFactory configFactory) {
        return new HttpClientsWorkloadReporter(metrics, httpClient, http2ClientHolder, configFactory);
    }

    @Bean
    public ClusterAssignmentCache clusterAssignmentCache(CuratorFramework curator,
                                                         ConfigFactory configFactory,
                                                         ConsumerAssignmentCache consumerAssignmentCache,
                                                         ZookeeperPaths zookeeperPaths,
                                                         SubscriptionIds subscriptionIds,
                                                         ConsumerNodesRegistry consumerNodesRegistry) {
        return new ClusterAssignmentCacheFactory(curator, configFactory, consumerAssignmentCache,
                zookeeperPaths, subscriptionIds, consumerNodesRegistry)
                .provide();
    }

    @Bean
    public ConsumerAssignmentCache consumerAssignmentCache(CuratorFramework curator,
                                                           ConfigFactory configFactory,
                                                           ZookeeperPaths zookeeperPaths,
                                                           SubscriptionsCache subscriptionsCache,
                                                           SubscriptionIds subscriptionIds) {
        return new ConsumerAssignmentCacheFactory(curator, configFactory, zookeeperPaths, subscriptionsCache, subscriptionIds)
                .provide();
    }

    @Bean
    public SubscriptionIds subscriptionIds(InternalNotificationsBus internalNotificationsBus,
                                           SubscriptionsCache subscriptionsCache,
                                           SubscriptionIdProvider subscriptionIdProvider,
                                           ConfigFactory configFactory) {
        return new SubscriptionIdsCacheFactory(internalNotificationsBus, subscriptionsCache, subscriptionIdProvider, configFactory)
                .provide();
    }

    @Bean
    public SubscriptionIdProvider subscriptionIdProvider(CuratorFramework curatorFramework,
                                                         ZookeeperPaths zookeeperPaths) {
        return new SubscriptionIdProviderFactory(curatorFramework, zookeeperPaths).provide();
    }

    @Bean
    public SubscriptionsCache subscriptionsCache(InternalNotificationsBus notificationsBus,
                                                 GroupRepository groupRepository,
                                                 TopicRepository topicRepository,
                                                 SubscriptionRepository subscriptionRepository) {
        return new SubscriptionCacheFactory(notificationsBus, groupRepository, topicRepository, subscriptionRepository)
                .provide();
    }

    @Bean
    public ConsumerNodesRegistry consumerNodesRegistry(CuratorFramework curatorFramework,
                                                       ConfigFactory configFactory,
                                                       ZookeeperPaths zookeeperPaths,
                                                       Clock clock) {
        return new ConsumerNodesRegistryFactory(curatorFramework, configFactory, zookeeperPaths, clock).provide();
    }

    @Bean
    public KafkaHeaderExtractor kafkaHeaderExtractor(ConfigFactory configFactory) {
        return new KafkaHeaderExtractor(configFactory);
    }

    //TODO
    @Bean
    public SslContextFactoryProvider sslContextFactoryProvider() {
        return new SslContextFactoryProvider();
    }

    @Bean
    public ConsumerMonitor consumerMonitor() {
        return new ConsumerMonitor();
    }

    @Bean
    public Retransmitter retransmitter(SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator,
                                       ConfigFactory configs) {
        return new Retransmitter(subscriptionOffsetChangeIndicator, configs);
    }

    @Bean
    public ConsumerPartitionAssignmentState consumerPartitionAssignmentState() {
        return new ConsumerPartitionAssignmentState();
    }

    @Bean
    public OffsetQueue offsetQueue(HermesMetrics metrics,
                                   ConfigFactory configFactory) {
        return new OffsetQueue(metrics, configFactory);
    }

    @Bean
    public MessageConverterResolver defaultMessageConverterResolver(AvroToJsonMessageConverter avroToJsonMessageConverter,
                                                                    NoOperationMessageConverter noOperationMessageConverter) {
        return new DefaultMessageConverterResolver(avroToJsonMessageConverter, noOperationMessageConverter);
    }

    //TODO: use interface?
    @Bean
    public AvroToJsonMessageConverter avroToJsonMessageConverter() {
        return new AvroToJsonMessageConverter();
    }

    @Bean
    public NoOperationMessageConverter noOperationMessageConverter() {
        return new NoOperationMessageConverter();
    }

    @Bean
    public ConsumerMessageSenderFactory consumerMessageSenderFactory(
            ConfigFactory configFactory, HermesMetrics hermesMetrics,
            MessageSenderFactory messageSenderFactory,
            Trackers trackers,
            FutureAsyncTimeout<MessageSendingResult> futureAsyncTimeout,
            UndeliveredMessageLog undeliveredMessageLog, Clock clock,
            InstrumentedExecutorServiceFactory instrumentedExecutorServiceFactory,
            ConsumerAuthorizationHandler consumerAuthorizationHandler) {
        return new ConsumerMessageSenderFactory(configFactory, hermesMetrics, messageSenderFactory, trackers,
                futureAsyncTimeout, undeliveredMessageLog, clock, instrumentedExecutorServiceFactory,
                consumerAuthorizationHandler);
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
    public OutputRateCalculatorFactory outputRateCalculatorFactory(ConfigFactory configFactory,
                                                                   MaxRateProviderFactory maxRateProviderFactory) {
        return new OutputRateCalculatorFactory(configFactory, maxRateProviderFactory);
    }

    @Bean
    public ConsumersExecutorService consumersExecutorService(ConfigFactory configFactory,
                                                             HermesMetrics hermesMetrics) {
        return new ConsumersExecutorService(configFactory, hermesMetrics);
    }

    @Bean
    public ConsumerRateLimitSupervisor consumerRateLimitSupervisor(ConfigFactory configFactory) {
        return new ConsumerRateLimitSupervisor(configFactory);
    }

    @Bean
    public ConsumerFactory consumerFactory(ReceiverFactory messageReceiverFactory,
                                           HermesMetrics hermesMetrics,
                                           ConfigFactory configFactory,
                                           ConsumerRateLimitSupervisor consumerRateLimitSupervisor,
                                           OutputRateCalculatorFactory outputRateCalculatorFactory,
                                           Trackers trackers,//TODO?
                                           OffsetQueue offsetQueue,
                                           ConsumerMessageSenderFactory consumerMessageSenderFactory,
                                           TopicRepository topicRepository,
                                           MessageConverterResolver messageConverterResolver,
                                           MessageBatchFactory byteBufferMessageBatchFactory,
                                           MessageContentWrapper messageContentWrapper,//TODO
                                           MessageBatchSenderFactory batchSenderFactory,
                                           ConsumerAuthorizationHandler consumerAuthorizationHandler,
                                           Clock clock) {
        return new ConsumerFactory(messageReceiverFactory, hermesMetrics, configFactory, consumerRateLimitSupervisor,
                outputRateCalculatorFactory, trackers, offsetQueue, consumerMessageSenderFactory, topicRepository,
                messageConverterResolver, byteBufferMessageBatchFactory, messageContentWrapper, batchSenderFactory,
                consumerAuthorizationHandler, clock);
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
    public HttpAuthorizationProviderFactory httpAuthorizationProviderFactory(OAuthAccessTokens accessTokens) {
        return new HttpAuthorizationProviderFactory(accessTokens);
    }

    @Bean
    public MessageSenderFactory messageSenderFactory() {
        return new MessageSenderFactory();
    }

    @Bean
    public ConsumersSupervisor nonblockingConsumersSupervisor(ConfigFactory configFactory,
                                                              ConsumersExecutorService executor,
                                                              ConsumerFactory consumerFactory,
                                                              OffsetQueue offsetQueue,
                                                              ConsumerPartitionAssignmentState consumerPartitionAssignmentState,
                                                              Retransmitter retransmitter,
                                                              UndeliveredMessageLogPersister undeliveredMessageLogPersister,
                                                              SubscriptionRepository subscriptionRepository,
                                                              HermesMetrics metrics,
                                                              ConsumerMonitor monitor,
                                                              Clock clock) {
        return new NonblockingConsumersSupervisor(configFactory, executor, consumerFactory, offsetQueue,
                consumerPartitionAssignmentState, retransmitter, undeliveredMessageLogPersister,
                subscriptionRepository, metrics, monitor, clock);
    }

    @Bean
    public HttpRequestFactoryProvider defaultHttpRequestFactoryProvider() {
        return new DefaultHttpRequestFactoryProvider();
    }

    @Bean
    public SendingResultHandlers defaultSendingResultHandlers() {
        return new DefaultSendingResultHandlers();
    }

    @Bean
    public HttpHeadersProvidersFactory emptyHttpHeadersProvidersFactory() {
        return new EmptyHttpHeadersProvidersFactory();
    }

    @Bean(name = "defaultHttpMessageSenderProvider")
    public ProtocolMessageSenderProvider jettyHttpMessageSenderProvider(@Named("http-1-client") HttpClient
                                                                                httpClient,
                                                                        Http2ClientHolder http2ClientHolder,
                                                                        EndpointAddressResolver endpointAddressResolver,
                                                                        MetadataAppender<Request> metadataAppender,
                                                                        HttpAuthorizationProviderFactory authorizationProviderFactory,
                                                                        HttpHeadersProvidersFactory httpHeadersProviderFactory,
                                                                        SendingResultHandlers sendingResultHandlers,
                                                                        HttpRequestFactoryProvider requestFactoryProvider) {
        return new JettyHttpMessageSenderProvider(httpClient, http2ClientHolder, endpointAddressResolver, metadataAppender,
                authorizationProviderFactory, httpHeadersProviderFactory, sendingResultHandlers, requestFactoryProvider);
    }

    @Bean(name = "defaultJmsMessageSenderProvider")
    public ProtocolMessageSenderProvider jmsHornetQMessageSenderProvider(ConfigFactory configFactory,
                                                                         MetadataAppender<Message> metadataAppender) {
        return new JmsHornetQMessageSenderProvider(configFactory, metadataAppender);
    }

    @Bean
    public EndpointAddressResolver interpolatingEndpointAddressResolver(UriInterpolator interpolator) {
        return new InterpolatingEndpointAddressResolver(interpolator);
    }

    @Bean
    public UriInterpolator messageBodyInterpolator() {
        return new MessageBodyInterpolator();
    }

    @Bean
    public BasicMessageContentReaderFactory basicMessageContentReaderFactory(MessageContentWrapper
                                                                                     messageContentWrapper,
                                                                             KafkaHeaderExtractor kafkaHeaderExtractor) {
        return new BasicMessageContentReaderFactory(messageContentWrapper, kafkaHeaderExtractor);
    }

    @Bean
    public ReceiverFactory kafkaMessageReceiverFactory(ConfigFactory configs,
                                                       MessageContentReaderFactory messageContentReaderFactory,
                                                       HermesMetrics hermesMetrics,
                                                       OffsetQueue offsetQueue,
                                                       Clock clock,
                                                       KafkaNamesMapper kafkaNamesMapper,
                                                       FilterChainFactory filterChainFactory,
                                                       Trackers trackers,
                                                       ConsumerPartitionAssignmentState consumerPartitionAssignmentState) {
        return new KafkaMessageReceiverFactory(configs, messageContentReaderFactory, hermesMetrics, offsetQueue, clock,
                kafkaNamesMapper, filterChainFactory, trackers, consumerPartitionAssignmentState);
    }

    @Bean
    public KafkaNamesMapper kafkaNamesMapper(ConfigFactory configFactory) {
        return new KafkaNamesMapperFactory(configFactory).provide();
    }

    @Bean
    public ConsumerHttpServer consumerHttpServer(ConfigFactory configFactory,
                                                 ConsumerMonitor monitor,
                                                 ObjectMapper mapper) throws IOException {
        return new ConsumerHttpServer(configFactory, monitor, mapper);
    }

    //TODO - use property instead?
    @Bean
    @Named("moduleName")//TODO - remove
    public String moduleName() {
        return "consumer";
    }

    @Bean
    public ConfigFactory configFactory() {
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
}
