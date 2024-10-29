package pl.allegro.tech.hermes.consumers.config;

import java.time.Clock;
import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
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
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimitSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculatorFactory;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRatePathSerializer;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateProviderFactory;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateRegistry;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ClusterAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentCache;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

@Configuration
@EnableConfigurationProperties({
  CommitOffsetProperties.class,
  SenderAsyncTimeoutProperties.class,
  RateProperties.class,
  BatchProperties.class,
  KafkaClustersProperties.class,
  WorkloadProperties.class,
  MaxRateProperties.class
})
public class ConsumerConfiguration {

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
  public MaxRateRegistry maxRateRegistry(
      MaxRateProperties maxRateProperties,
      KafkaClustersProperties kafkaClustersProperties,
      WorkloadProperties workloadProperties,
      CuratorFramework curator,
      ZookeeperPaths zookeeperPaths,
      SubscriptionIds subscriptionIds,
      ConsumerAssignmentCache assignmentCache,
      ClusterAssignmentCache clusterAssignmentCache,
      DatacenterNameProvider datacenterNameProvider) {
    KafkaProperties kafkaProperties =
        kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
    return new MaxRateRegistry(
        maxRateProperties.getRegistryBinaryEncoder().getHistoryBufferSizeBytes(),
        maxRateProperties.getRegistryBinaryEncoder().getMaxRateBufferSizeBytes(),
        workloadProperties.getNodeId(),
        kafkaProperties.getClusterName(),
        clusterAssignmentCache,
        assignmentCache,
        curator,
        zookeeperPaths,
        subscriptionIds);
  }

  @Bean(initMethod = "start", destroyMethod = "stop")
  public MaxRateSupervisor maxRateSupervisor(
      MaxRateProperties maxRateProperties,
      ClusterAssignmentCache clusterAssignmentCache,
      MaxRateRegistry maxRateRegistry,
      ConsumerNodesRegistry consumerNodesRegistry,
      SubscriptionsCache subscriptionsCache,
      MetricsFacade metrics,
      Clock clock) {
    return new MaxRateSupervisor(
        maxRateProperties,
        clusterAssignmentCache,
        maxRateRegistry,
        consumerNodesRegistry,
        subscriptionsCache,
        metrics,
        clock);
  }

  @Bean
  public ConsumerRateLimitSupervisor consumerRateLimitSupervisor(RateProperties rateProperties) {
    return new ConsumerRateLimitSupervisor(rateProperties.getLimiterSupervisorPeriod());
  }

  @Bean
  public MaxRateProviderFactory maxRateProviderFactory(
      MaxRateProperties maxRateProperties,
      MaxRateRegistry maxRateRegistry,
      MaxRateSupervisor maxRateSupervisor,
      WorkloadProperties workloadProperties) {
    return new MaxRateProviderFactory(
        maxRateProperties, workloadProperties.getNodeId(), maxRateRegistry, maxRateSupervisor);
  }

  @Bean
  public AvroToJsonMessageConverter avroToJsonMessageConverter() {
    return new AvroToJsonMessageConverter();
  }

  @Bean
  public OutputRateCalculatorFactory outputRateCalculatorFactory(
      RateProperties rateProperties, MaxRateProviderFactory maxRateProviderFactory) {
    return new OutputRateCalculatorFactory(rateProperties, maxRateProviderFactory);
  }

  @Bean
  public MessageBatchFactory messageBatchFactory(
      MetricsFacade metrics, Clock clock, BatchProperties batchProperties) {
    return new ByteBufferMessageBatchFactory(
        batchProperties.getPoolableSize(), batchProperties.getMaxPoolSize(), clock, metrics);
  }

  @Bean
  public MessageConverterResolver defaultMessageConverterResolver(
      AvroToJsonMessageConverter avroToJsonMessageConverter,
      NoOperationMessageConverter noOperationMessageConverter) {
    return new DefaultMessageConverterResolver(
        avroToJsonMessageConverter, noOperationMessageConverter);
  }

  @Bean
  public ConsumerMessageSenderFactory consumerMessageSenderFactory(
      KafkaClustersProperties kafkaClustersProperties,
      MessageSenderFactory messageSenderFactory,
      Trackers trackers,
      FutureAsyncTimeout futureAsyncTimeout,
      UndeliveredMessageLog undeliveredMessageLog,
      Clock clock,
      InstrumentedExecutorServiceFactory instrumentedExecutorServiceFactory,
      ConsumerAuthorizationHandler consumerAuthorizationHandler,
      SenderAsyncTimeoutProperties senderAsyncTimeoutProperties,
      RateProperties rateProperties,
      DatacenterNameProvider datacenterNameProvider) {
    KafkaProperties kafkaProperties =
        kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
    return new ConsumerMessageSenderFactory(
        kafkaProperties.getClusterName(),
        messageSenderFactory,
        trackers,
        futureAsyncTimeout,
        undeliveredMessageLog,
        clock,
        instrumentedExecutorServiceFactory,
        consumerAuthorizationHandler,
        senderAsyncTimeoutProperties.getMilliseconds(),
        rateProperties.getLimiterReportingThreadPoolSize(),
        rateProperties.isLimiterReportingThreadMonitoringEnabled());
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
