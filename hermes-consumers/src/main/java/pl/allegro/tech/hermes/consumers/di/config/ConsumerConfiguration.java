package pl.allegro.tech.hermes.consumers.di.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.GenericApplicationContext;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerAuthorizationHandler;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerMessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.batch.ByteBufferMessageBatchFactory;
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
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthSubscriptionAccessTokens;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthSubscriptionHandlerFactory;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthTokenRequestRateLimiterFactory;
import pl.allegro.tech.hermes.consumers.consumer.oauth.client.OAuthClient;
import pl.allegro.tech.hermes.consumers.consumer.oauth.client.OAuthHttpClient;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimitSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculatorFactory;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.ConsumerMaxRateRegistryType;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.FlatBinaryMaxRateRegistry;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.HierarchicalCacheMaxRateRegistry;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRatePathSerializer;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateProviderFactory;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateRegistry;
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
import pl.allegro.tech.hermes.consumers.consumer.sender.http.Http2ClientHolder;
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
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;
import pl.allegro.tech.hermes.consumers.hooks.SpringHooksHandler;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ClusterAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentCache;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import javax.inject.Named;
import javax.jms.Message;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import static org.slf4j.LoggerFactory.getLogger;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP2_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_MONITORING;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_SIZE;

@Configuration
public class ConsumerConfiguration {
    private static final Logger logger = getLogger(ConsumerConfiguration.class);

    @Bean
    public ApplicationContext applicationContext() {
        return new GenericApplicationContext();
    }

    @Bean
    public MaxRatePathSerializer maxRatePathSerializer() {
        return new MaxRatePathSerializer();
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
    public SendingResultHandlers defaultSendingResultHandlers() {
        return new DefaultSendingResultHandlers();
    }

    @Bean
    public HttpHeadersProvidersFactory emptyHttpHeadersProvidersFactory() {
        return new EmptyHttpHeadersProvidersFactory();
    }

    @Bean
    public MessageSenderFactory messageSenderFactory() {
        return new MessageSenderFactory();
    }

    @Bean
    public HttpRequestFactoryProvider defaultHttpRequestFactoryProvider() {
        return new DefaultHttpRequestFactoryProvider();
    }

    @Bean
    public NoOperationMessageConverter noOperationMessageConverter() {
        return new NoOperationMessageConverter();
    }

    @Bean
    public ConsumerPartitionAssignmentState consumerPartitionAssignmentState() {
        return new ConsumerPartitionAssignmentState();
    }

    @Bean
    public SslContextFactoryProvider sslContextFactoryProvider(Optional<SslContextFactory> sslContextFactory, ConfigFactory configFactory) {
        return new SslContextFactoryProvider(sslContextFactory.orElse(null), configFactory);
    }

    @Bean
    public OAuthTokenRequestRateLimiterFactory oAuthTokenRequestRateLimiterFactory(OAuthProviderRepository oAuthProviderRepository,
                                                                                   ConfigFactory configFactory) {
        return new OAuthTokenRequestRateLimiterFactory(oAuthProviderRepository, configFactory);
    }

    @Bean
    public FutureAsyncTimeout<MessageSendingResult> futureAsyncTimeoutFactory(ConfigFactory configFactory,
                                                                              InstrumentedExecutorServiceFactory executorFactory) {
        ScheduledExecutorService timeoutExecutorService = executorFactory.getScheduledExecutorService(
                "async-timeout",
                configFactory.getIntProperty(CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_SIZE),
                configFactory.getBooleanProperty(CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_MONITORING)
        );
        return new FutureAsyncTimeout<>(MessageSendingResult::failedResult, timeoutExecutorService);
    }

    @Bean
    public OAuthAccessTokens oAuthSubscriptionAccessTokens(OAuthAccessTokensLoader tokenLoader,
                                                           ConfigFactory configFactory) {
        return new OAuthSubscriptionAccessTokens(tokenLoader, configFactory);
    }

    @Bean
    public ConsumerAuthorizationHandler oAuthConsumerAuthorizationHandler(OAuthSubscriptionHandlerFactory handlerFactory,
                                                                          ConfigFactory configFactory,
                                                                          OAuthProvidersNotifyingCache oAuthProvidersCache) {
        return new OAuthConsumerAuthorizationHandler(handlerFactory, configFactory, oAuthProvidersCache);
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
        ConsumerMaxRateRegistryType type;
        try {
            String typeString = configFactory.getStringProperty(Configs.CONSUMER_MAXRATE_REGISTRY_TYPE);
            type = ConsumerMaxRateRegistryType.fromString(typeString);
        } catch (Exception e) {
            logger.error("Could not configure max rate registry", e);
            throw e;
        }
        logger.info("Max rate registry type chosen: {}", type.getConfigValue());

        switch (type) {
            case HIERARCHICAL:
                return new HierarchicalCacheMaxRateRegistry(
                        configFactory,
                        curator,
                        objectMapper,
                        zookeeperPaths,
                        pathSerializer,
                        subscriptionCache
                );
            case FLAT_BINARY:
                return new FlatBinaryMaxRateRegistry(
                        configFactory,
                        clusterAssignmentCache,
                        assignmentCache,
                        curator,
                        zookeeperPaths,
                        subscriptionIds
                );
            default:
                throw new UnsupportedOperationException("Max-rate type not supported.");
        }
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
        return new MaxRateSupervisor(
                configFactory,
                clusterAssignmentCache,
                maxRateRegistry,
                consumerNodesRegistry,
                subscriptionsCache,
                zookeeperPaths,
                metrics,
                clock
        );
    }

    @Bean
    public OffsetQueue offsetQueue(HermesMetrics metrics,
                                   ConfigFactory configFactory) {
        return new OffsetQueue(metrics, configFactory);
    }

    @Bean(name = { "defaultHttpMessageSenderProvider", "defaultHttpsMessageSenderProvider" })
    public ProtocolMessageSenderProvider jettyHttpMessageSenderProvider(@Named("http-1-client") HttpClient
                                                                                httpClient,
                                                                        Http2ClientHolder http2ClientHolder,
                                                                        EndpointAddressResolver endpointAddressResolver,
                                                                        MetadataAppender<Request> metadataAppender,
                                                                        HttpAuthorizationProviderFactory authorizationProviderFactory,
                                                                        HttpHeadersProvidersFactory httpHeadersProviderFactory,
                                                                        SendingResultHandlers sendingResultHandlers,
                                                                        HttpRequestFactoryProvider requestFactoryProvider) {
        return new JettyHttpMessageSenderProvider(
                httpClient,
                http2ClientHolder,
                endpointAddressResolver,
                metadataAppender,
                authorizationProviderFactory,
                httpHeadersProviderFactory,
                sendingResultHandlers,
                requestFactoryProvider
        );
    }

    @Bean
    public ConsumerRateLimitSupervisor consumerRateLimitSupervisor(ConfigFactory configFactory) {
        return new ConsumerRateLimitSupervisor(configFactory);
    }

    @Bean
//    @ConditionalOnMissingBean//TODO: add condition
    public EndpointAddressResolver interpolatingEndpointAddressResolver(UriInterpolator interpolator) {
        return new InterpolatingEndpointAddressResolver(interpolator);
    }

    @Bean
    public MessageBatchSenderFactory httpMessageBatchSenderFactory(ConfigFactory configFactory,
                                                                   SendingResultHandlers resultHandlers) {
        return new HttpMessageBatchSenderFactory(configFactory, resultHandlers);
    }

    @Bean
    public KafkaHeaderExtractor kafkaHeaderExtractor(ConfigFactory configFactory) {
        return new KafkaHeaderExtractor(configFactory);
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
        return new KafkaMessageReceiverFactory(
                configs,
                messageContentReaderFactory,
                hermesMetrics,
                offsetQueue,
                clock,
                kafkaNamesMapper,
                filterChainFactory,
                trackers,
                consumerPartitionAssignmentState
        );
    }

    @Bean
    public OAuthClient oAuthHttpClient(@Named("oauth-http-client") HttpClient httpClient,
                                       ObjectMapper objectMapper) {
        return new OAuthHttpClient(httpClient, objectMapper);
    }

    @Bean
    public MaxRateProviderFactory maxRateProviderFactory(ConfigFactory configFactory,
                                                         MaxRateRegistry maxRateRegistry,
                                                         MaxRateSupervisor maxRateSupervisor,
                                                         HermesMetrics metrics) {
        return new MaxRateProviderFactory(configFactory, maxRateRegistry, maxRateSupervisor, metrics);
    }

    //TODO: use interface?
    @Bean
    public AvroToJsonMessageConverter avroToJsonMessageConverter() {
        return new AvroToJsonMessageConverter();
    }

    @Bean
    public OAuthSubscriptionHandlerFactory oAuthSubscriptionHandlerFactory(SubscriptionRepository subscriptionRepository,
                                                                           OAuthAccessTokens accessTokens,
                                                                           OAuthTokenRequestRateLimiterFactory rateLimiterLoader) {
        return new OAuthSubscriptionHandlerFactory(subscriptionRepository, accessTokens, rateLimiterLoader);
    }

    @Bean
    public HttpAuthorizationProviderFactory httpAuthorizationProviderFactory(OAuthAccessTokens accessTokens) {
        return new HttpAuthorizationProviderFactory(accessTokens);
    }

    @Bean
    public OutputRateCalculatorFactory outputRateCalculatorFactory(ConfigFactory configFactory,
                                                                   MaxRateProviderFactory maxRateProviderFactory) {
        return new OutputRateCalculatorFactory(configFactory, maxRateProviderFactory);
    }

    @Bean
    public BasicMessageContentReaderFactory basicMessageContentReaderFactory(MessageContentWrapper
                                                                                     messageContentWrapper,
                                                                             KafkaHeaderExtractor kafkaHeaderExtractor) {
        return new BasicMessageContentReaderFactory(messageContentWrapper, kafkaHeaderExtractor);
    }

    @Bean
    public MessageBatchFactory messageBatchFactory(HermesMetrics hermesMetrics,
                                                   Clock clock,
                                                   ConfigFactory configFactory) {
        int poolableSize = configFactory.getIntProperty(Configs.CONSUMER_BATCH_POOLABLE_SIZE);
        int maxPoolSize = configFactory.getIntProperty(Configs.CONSUMER_BATCH_MAX_POOL_SIZE);
        return new ByteBufferMessageBatchFactory(poolableSize, maxPoolSize, clock, hermesMetrics);
    }

    @Bean
    public MessageConverterResolver defaultMessageConverterResolver(AvroToJsonMessageConverter avroToJsonMessageConverter,
                                                                    NoOperationMessageConverter noOperationMessageConverter) {
        return new DefaultMessageConverterResolver(avroToJsonMessageConverter, noOperationMessageConverter);
    }

    @Bean(name = "http-1-client")
    public HttpClient http1Client(HttpClientsFactory httpClientsFactory) {
        return httpClientsFactory.createClientForHttp1("jetty-http-client");
    }

    @Bean(name = "oauth-http-client")
    public HttpClient oauthHttpClient(HttpClientsFactory httpClientsFactory) {
        return httpClientsFactory.createClientForHttp1("jetty-http-oauthclient");
    }

    @Bean
    public Http2ClientHolder http2ClientHolder(HttpClientsFactory httpClientsFactory,
                                               ConfigFactory configFactory) {
        if (!configFactory.getBooleanProperty(CONSUMER_HTTP2_ENABLED)) {
            return new Http2ClientHolder(null);
        } else {
            return new Http2ClientHolder(httpClientsFactory.createClientForHttp2());
        }
    }

    @Bean
    public HttpClientsFactory httpClientsFactory(ConfigFactory configFactory,
                                                 InstrumentedExecutorServiceFactory executorFactory,
                                                 SslContextFactoryProvider sslContextFactoryProvider) {
        return new HttpClientsFactory(configFactory, executorFactory, sslContextFactoryProvider);
    }

    @Bean
    public OAuthAccessTokensLoader oAuthAccessTokensLoader(SubscriptionRepository subscriptionRepository,
                                                           OAuthProviderRepository oAuthProviderRepository,
                                                           OAuthClient oAuthClient,
                                                           HermesMetrics metrics) {
        return new OAuthAccessTokensLoader(subscriptionRepository, oAuthProviderRepository, oAuthClient, metrics);
    }

    @Bean
    public ConsumerMessageSenderFactory consumerMessageSenderFactory(ConfigFactory configFactory,
                                                                     HermesMetrics hermesMetrics,
                                                                     MessageSenderFactory messageSenderFactory,
                                                                     Trackers trackers,
                                                                     FutureAsyncTimeout<MessageSendingResult> futureAsyncTimeout,
                                                                     UndeliveredMessageLog undeliveredMessageLog, Clock clock,
                                                                     InstrumentedExecutorServiceFactory instrumentedExecutorServiceFactory,
                                                                     ConsumerAuthorizationHandler consumerAuthorizationHandler) {
        return new ConsumerMessageSenderFactory(
                configFactory,
                hermesMetrics,
                messageSenderFactory,
                trackers,
                futureAsyncTimeout,
                undeliveredMessageLog,
                clock,
                instrumentedExecutorServiceFactory,
                consumerAuthorizationHandler
        );
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)//TODO - bindFactory, raczej singleton
    public OAuthProvidersNotifyingCache oAuthProvidersNotifyingCache(@Named(CuratorType.HERMES) CuratorFramework curator,
                                                                     ZookeeperPaths paths,
                                                                     ObjectMapper objectMapper) {
        String path = paths.oAuthProvidersPath();
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("oauth-providers-notifying-cache-%d").build();
        ExecutorService executorService = Executors.newSingleThreadExecutor(threadFactory);
        OAuthProvidersNotifyingCache cache = new OAuthProvidersNotifyingCache(curator, path, executorService, objectMapper);
        try {
            cache.start();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to start Zookeeper cache for path " + path, e);
        }
        return cache;
    }

    @Bean(name = "defaultJmsMessageSenderProvider")
    public ProtocolMessageSenderProvider jmsHornetQMessageSenderProvider(ConfigFactory configFactory,
                                                                         MetadataAppender<Message> metadataAppender) {
        return new JmsHornetQMessageSenderProvider(configFactory, metadataAppender);
    }

    @Bean
    public HttpClientsWorkloadReporter httpClientsWorkloadReporter(HermesMetrics metrics,
                                                                   @Named("http-1-client") HttpClient httpClient,
                                                                   Http2ClientHolder http2ClientHolder,
                                                                   ConfigFactory configFactory) {
        return new HttpClientsWorkloadReporter(metrics, httpClient, http2ClientHolder, configFactory);
    }

    @Bean
//    @ConditionalOnMissingBean
    public SpringHooksHandler prodSpringHooksHandler() {
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
    public UriInterpolator messageBodyInterpolator() {
        return new MessageBodyInterpolator();
    }

    @Bean
    public Trackers trackers() {
        return new Trackers(new ArrayList<>());
    }
}
