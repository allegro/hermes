package pl.allegro.tech.hermes.consumers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.eclipse.jetty.client.HttpClient;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
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
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientsFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ClusterAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.time.Clock;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
@EnableConfigurationProperties({
        CommitOffsetProperties.class,
        SenderAsyncTimeoutProperties.class,
        RateProperties.class,
        BatchProperties.class,
        KafkaProperties.class
})
public class ConsumerConfiguration {
    private static final Logger logger = getLogger(ConsumerConfiguration.class);

    @Bean
    public MaxRatePathSerializer maxRatePathSerializer() {
        return new MaxRatePathSerializer();
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
    public MaxRateRegistry maxRateRegistry(ConfigFactory configFactory,
                                           KafkaProperties kafkaProperties,
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
                        kafkaProperties.getClusterName(),
                        curator,
                        objectMapper,
                        zookeeperPaths,
                        pathSerializer,
                        subscriptionCache
                );
            case FLAT_BINARY:
                return new FlatBinaryMaxRateRegistry(
                        configFactory,
                        kafkaProperties.getClusterName(),
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

    @Bean(initMethod = "start", destroyMethod = "stop")
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
                                   CommitOffsetProperties commitOffsetProperties) {
        return new OffsetQueue(metrics, commitOffsetProperties.getQueuesSize(), commitOffsetProperties.isQueuesInflightDrainFullEnabled());
    }

    @Bean
    public ConsumerRateLimitSupervisor consumerRateLimitSupervisor(RateProperties rateProperties) {
        return new ConsumerRateLimitSupervisor(rateProperties.getLimiterSupervisorPeriod());
    }

    @Bean
    public MaxRateProviderFactory maxRateProviderFactory(ConfigFactory configFactory,
                                                         MaxRateRegistry maxRateRegistry,
                                                         MaxRateSupervisor maxRateSupervisor,
                                                         HermesMetrics metrics) {
        return new MaxRateProviderFactory(configFactory, maxRateRegistry, maxRateSupervisor, metrics);
    }

    @Bean
    public AvroToJsonMessageConverter avroToJsonMessageConverter() {
        return new AvroToJsonMessageConverter();
    }

    @Bean
    public OutputRateCalculatorFactory outputRateCalculatorFactory(RateProperties rateProperties,
                                                                   MaxRateProviderFactory maxRateProviderFactory) {
        return new OutputRateCalculatorFactory(rateProperties.toRateCalculatorParameters(), maxRateProviderFactory);
    }

    @Bean
    public MessageBatchFactory messageBatchFactory(HermesMetrics hermesMetrics,
                                                   Clock clock,
                                                   BatchProperties batchProperties) {
        return new ByteBufferMessageBatchFactory(batchProperties.getPoolableSize(), batchProperties.getMaxPoolSize(), clock, hermesMetrics);
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
    public ConsumerMessageSenderFactory consumerMessageSenderFactory(KafkaProperties kafkaProperties,
                                                                     HermesMetrics hermesMetrics,
                                                                     MessageSenderFactory messageSenderFactory,
                                                                     Trackers trackers,
                                                                     FutureAsyncTimeout<MessageSendingResult> futureAsyncTimeout,
                                                                     UndeliveredMessageLog undeliveredMessageLog, Clock clock,
                                                                     InstrumentedExecutorServiceFactory instrumentedExecutorServiceFactory,
                                                                     ConsumerAuthorizationHandler consumerAuthorizationHandler,
                                                                     SenderAsyncTimeoutProperties senderAsyncTimeoutProperties,
                                                                     RateProperties rateProperties) {
        return new ConsumerMessageSenderFactory(
                kafkaProperties.getClusterName(),
                hermesMetrics,
                messageSenderFactory,
                trackers,
                futureAsyncTimeout,
                undeliveredMessageLog,
                clock,
                instrumentedExecutorServiceFactory,
                consumerAuthorizationHandler,
                senderAsyncTimeoutProperties.getMilliseconds(),
                rateProperties.getLimiterReportingThreadPoolSize(),
                rateProperties.isLimiterReportingThreadMonitoringEnabled()
        );
    }

    @Bean
    public UriInterpolator messageBodyInterpolator() {
        return new MessageBodyInterpolator();
    }

    @Bean(destroyMethod = "close")
    public Trackers trackers(List<LogRepository> repositories) {
        return new Trackers(repositories);
    }
}
