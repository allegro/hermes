package pl.allegro.tech.hermes.consumers.config;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.concurrent.ExecutorServiceFactory;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.config.WorkloadProperties.TargetWeightCalculationStrategy.UnknownTargetWeightCalculationStrategyException;
import pl.allegro.tech.hermes.consumers.config.WorkloadProperties.WeightedWorkBalancingProperties;
import pl.allegro.tech.hermes.consumers.config.WorkloadProperties.WorkBalancingStrategy.UnknownWorkBalancingStrategyException;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerAuthorizationHandler;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerMessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchFactory;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecordersRegistry;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimitSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.rate.calculator.OutputRateCalculatorFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSenderFactory;
import pl.allegro.tech.hermes.consumers.health.ConsumerMonitor;
import pl.allegro.tech.hermes.consumers.message.undelivered.UndeliveredMessageLogPersister;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerFactory;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersExecutorService;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.NonblockingConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.monitor.ConsumersRuntimeMonitor;
import pl.allegro.tech.hermes.consumers.supervisor.process.Retransmitter;
import pl.allegro.tech.hermes.consumers.supervisor.workload.BalancingListener;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ClusterAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.workload.NoOpBalancingListener;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkBalancer;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkloadSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.SelectiveWorkBalancer;
import pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.AvgTargetWeightCalculator;
import pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.ConsumerNodeLoadRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.CurrentLoadProvider;
import pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.NoOpConsumerNodeLoadRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.ScoringTargetWeightCalculator;
import pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.SubscriptionProfileRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.TargetWeightCalculator;
import pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.WeightedWorkBalancer;
import pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.WeightedWorkBalancingListener;
import pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.WeightedWorkloadMetricsReporter;
import pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.ZookeeperConsumerNodeLoadRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.workload.weighted.ZookeeperSubscriptionProfileRegistry;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

@Configuration
@EnableConfigurationProperties({
  CommitOffsetProperties.class,
  KafkaClustersProperties.class,
  WorkloadProperties.class,
  CommonConsumerProperties.class
})
public class SupervisorConfiguration {
  private static final Logger logger = getLogger(SupervisorConfiguration.class);

  @Bean(initMethod = "start", destroyMethod = "shutdown")
  public WorkloadSupervisor workloadSupervisor(
      InternalNotificationsBus notificationsBus,
      ConsumerNodesRegistry consumerNodesRegistry,
      ConsumerAssignmentRegistry assignmentRegistry,
      ConsumerAssignmentCache consumerAssignmentCache,
      ClusterAssignmentCache clusterAssignmentCache,
      SubscriptionsCache subscriptionsCache,
      ConsumersSupervisor supervisor,
      ZookeeperAdminCache adminCache,
      MetricsFacade metrics,
      WorkloadProperties workloadProperties,
      KafkaClustersProperties kafkaClustersProperties,
      WorkloadConstraintsRepository workloadConstraintsRepository,
      DatacenterNameProvider datacenterNameProvider,
      WorkBalancer workBalancer,
      BalancingListener balancingListener) {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder()
            .setNameFormat("AssignmentExecutor-%d")
            .setUncaughtExceptionHandler(
                (t, e) -> logger.error("AssignmentExecutor failed {}", t.getName(), e))
            .build();
    ExecutorService assignmentExecutor =
        newFixedThreadPool(
            workloadProperties.getAssignmentProcessingThreadPoolSize(), threadFactory);
    KafkaProperties kafkaProperties =
        kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
    return new WorkloadSupervisor(
        supervisor,
        notificationsBus,
        subscriptionsCache,
        consumerAssignmentCache,
        assignmentRegistry,
        clusterAssignmentCache,
        consumerNodesRegistry,
        adminCache,
        assignmentExecutor,
        workloadProperties,
        kafkaProperties.getClusterName(),
        metrics,
        workloadConstraintsRepository,
        workBalancer,
        balancingListener);
  }

  @Bean
  public WorkBalancer workBalancer(
      WorkloadProperties workloadProperties,
      Clock clock,
      CurrentLoadProvider currentLoadProvider,
      TargetWeightCalculator targetWeightCalculator) {
    switch (workloadProperties.getWorkBalancingStrategy()) {
      case SELECTIVE:
        return new SelectiveWorkBalancer();
      case WEIGHTED:
        WeightedWorkBalancingProperties weightedWorkBalancingProperties =
            workloadProperties.getWeightedWorkBalancing();
        return new WeightedWorkBalancer(
            clock,
            weightedWorkBalancingProperties.getStabilizationWindowSize(),
            weightedWorkBalancingProperties.getMinSignificantChangePercent(),
            currentLoadProvider,
            targetWeightCalculator);
      default:
        throw new UnknownWorkBalancingStrategyException();
    }
  }

  @Bean(initMethod = "start", destroyMethod = "stop")
  public ConsumerNodeLoadRegistry consumerNodeLoadRegistry(
      CuratorFramework curator,
      SubscriptionIds subscriptionIds,
      ZookeeperPaths zookeeperPaths,
      WorkloadProperties workloadProperties,
      KafkaClustersProperties kafkaClustersProperties,
      DatacenterNameProvider datacenterNameProvider,
      ExecutorServiceFactory executorServiceFactory,
      Clock clock,
      MetricsFacade metrics) {
    switch (workloadProperties.getWorkBalancingStrategy()) {
      case SELECTIVE:
        return new NoOpConsumerNodeLoadRegistry();
      case WEIGHTED:
        KafkaProperties kafkaProperties =
            kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
        WeightedWorkBalancingProperties weightedWorkBalancing =
            workloadProperties.getWeightedWorkBalancing();
        return new ZookeeperConsumerNodeLoadRegistry(
            curator,
            subscriptionIds,
            zookeeperPaths,
            workloadProperties.getNodeId(),
            kafkaProperties.getClusterName(),
            weightedWorkBalancing.getLoadReportingInterval(),
            executorServiceFactory,
            clock,
            metrics,
            weightedWorkBalancing.getConsumerLoadEncoderBufferSizeBytes());
      default:
        throw new UnknownWorkBalancingStrategyException();
    }
  }

  @Bean
  public TargetWeightCalculator targetWeightCalculator(
      WorkloadProperties workloadProperties,
      WeightedWorkloadMetricsReporter metricsReporter,
      Clock clock) {
    WeightedWorkBalancingProperties weightedWorkBalancing =
        workloadProperties.getWeightedWorkBalancing();
    switch (weightedWorkBalancing.getTargetWeightCalculationStrategy()) {
      case AVG:
        return new AvgTargetWeightCalculator(metricsReporter);
      case SCORING:
        return new ScoringTargetWeightCalculator(
            metricsReporter,
            clock,
            weightedWorkBalancing.getWeightWindowSize(),
            weightedWorkBalancing.getScoringGain());
      default:
        throw new UnknownTargetWeightCalculationStrategyException();
    }
  }

  @Bean
  public BalancingListener balancingListener(
      ConsumerNodeLoadRegistry consumerNodeLoadRegistry,
      SubscriptionProfileRegistry subscriptionProfileRegistry,
      WorkloadProperties workloadProperties,
      CurrentLoadProvider currentLoadProvider,
      WeightedWorkloadMetricsReporter weightedWorkloadMetrics,
      Clock clock) {
    switch (workloadProperties.getWorkBalancingStrategy()) {
      case SELECTIVE:
        return new NoOpBalancingListener();
      case WEIGHTED:
        return new WeightedWorkBalancingListener(
            consumerNodeLoadRegistry,
            subscriptionProfileRegistry,
            currentLoadProvider,
            weightedWorkloadMetrics,
            clock,
            workloadProperties.getWeightedWorkBalancing().getWeightWindowSize());
      default:
        throw new UnknownWorkBalancingStrategyException();
    }
  }

  @Bean
  public CurrentLoadProvider currentLoadProvider() {
    return new CurrentLoadProvider();
  }

  @Bean
  public WeightedWorkloadMetricsReporter weightedWorkloadMetrics(MetricsFacade metrics) {
    return new WeightedWorkloadMetricsReporter(metrics);
  }

  @Bean
  public SubscriptionProfileRegistry subscriptionProfileRegistry(
      CuratorFramework curator,
      SubscriptionIds subscriptionIds,
      ZookeeperPaths zookeeperPaths,
      WorkloadProperties workloadProperties,
      KafkaClustersProperties kafkaClustersProperties,
      DatacenterNameProvider datacenterNameProvider) {
    KafkaProperties kafkaProperties =
        kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
    WeightedWorkBalancingProperties weightedWorkBalancing =
        workloadProperties.getWeightedWorkBalancing();
    return new ZookeeperSubscriptionProfileRegistry(
        curator,
        subscriptionIds,
        zookeeperPaths,
        kafkaProperties.getClusterName(),
        weightedWorkBalancing.getSubscriptionProfilesEncoderBufferSizeBytes());
  }

  @Bean
  public Retransmitter retransmitter(
      SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator,
      KafkaClustersProperties kafkaClustersProperties,
      DatacenterNameProvider datacenterNameProvider) {
    KafkaProperties kafkaProperties =
        kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);

    return new Retransmitter(subscriptionOffsetChangeIndicator, kafkaProperties.getClusterName());
  }

  @Bean
  public ConsumerFactory consumerFactory(
      ReceiverFactory messageReceiverFactory,
      MetricsFacade metrics,
      CommonConsumerProperties commonConsumerProperties,
      ConsumerRateLimitSupervisor consumerRateLimitSupervisor,
      OutputRateCalculatorFactory outputRateCalculatorFactory,
      Trackers trackers,
      ConsumerMessageSenderFactory consumerMessageSenderFactory,
      TopicRepository topicRepository,
      MessageConverterResolver messageConverterResolver,
      MessageBatchFactory byteBufferMessageBatchFactory,
      CompositeMessageContentWrapper compositeMessageContentWrapper,
      MessageBatchSenderFactory batchSenderFactory,
      ConsumerAuthorizationHandler consumerAuthorizationHandler,
      Clock clock,
      SubscriptionLoadRecordersRegistry subscriptionLoadRecordersRegistry,
      ConsumerPartitionAssignmentState consumerPartitionAssignmentState,
      CommitOffsetProperties commitOffsetProperties) {
    return new ConsumerFactory(
        messageReceiverFactory,
        metrics,
        commonConsumerProperties,
        consumerRateLimitSupervisor,
        outputRateCalculatorFactory,
        trackers,
        consumerMessageSenderFactory,
        topicRepository,
        messageConverterResolver,
        byteBufferMessageBatchFactory,
        compositeMessageContentWrapper,
        batchSenderFactory,
        consumerAuthorizationHandler,
        clock,
        subscriptionLoadRecordersRegistry,
        consumerPartitionAssignmentState,
        commitOffsetProperties.getPeriod(),
        commitOffsetProperties.getQueuesSize());
  }

  @Bean
  public ConsumersExecutorService consumersExecutorService(
      CommonConsumerProperties commonConsumerProperties, MetricsFacade metrics) {
    return new ConsumersExecutorService(commonConsumerProperties.getThreadPoolSize(), metrics);
  }

  @Bean
  public ConsumersSupervisor nonblockingConsumersSupervisor(
      CommonConsumerProperties commonConsumerProperties,
      ConsumersExecutorService executor,
      ConsumerFactory consumerFactory,
      ConsumerPartitionAssignmentState consumerPartitionAssignmentState,
      Retransmitter retransmitter,
      UndeliveredMessageLogPersister undeliveredMessageLogPersister,
      SubscriptionRepository subscriptionRepository,
      MetricsFacade metrics,
      ConsumerMonitor monitor,
      Clock clock) {
    return new NonblockingConsumersSupervisor(
        commonConsumerProperties,
        executor,
        consumerFactory,
        consumerPartitionAssignmentState,
        retransmitter,
        undeliveredMessageLogPersister,
        subscriptionRepository,
        metrics,
        monitor,
        clock);
  }

  @Bean(initMethod = "start", destroyMethod = "shutdown")
  public ConsumersRuntimeMonitor consumersRuntimeMonitor(
      ConsumersSupervisor consumerSupervisor,
      WorkloadSupervisor workloadSupervisor,
      MetricsFacade metrics,
      SubscriptionsCache subscriptionsCache,
      WorkloadProperties workloadProperties) {
    return new ConsumersRuntimeMonitor(
        consumerSupervisor,
        workloadSupervisor,
        metrics,
        subscriptionsCache,
        workloadProperties.getMonitorScanInterval());
  }

  @Bean
  public ConsumerAssignmentRegistry consumerAssignmentRegistry(
      CuratorFramework curator,
      WorkloadProperties workloadProperties,
      KafkaClustersProperties kafkaClustersProperties,
      ZookeeperPaths zookeeperPaths,
      SubscriptionIds subscriptionIds,
      DatacenterNameProvider datacenterNameProvider) {
    KafkaProperties kafkaProperties =
        kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
    return new ConsumerAssignmentRegistry(
        curator,
        workloadProperties.getRegistryBinaryEncoderAssignmentsBufferSizeBytes(),
        kafkaProperties.getClusterName(),
        zookeeperPaths,
        subscriptionIds);
  }

  @Bean
  public ClusterAssignmentCache clusterAssignmentCache(
      CuratorFramework curator,
      KafkaClustersProperties kafkaClustersProperties,
      ZookeeperPaths zookeeperPaths,
      SubscriptionIds subscriptionIds,
      ConsumerNodesRegistry consumerNodesRegistry,
      DatacenterNameProvider datacenterNameProvider) {
    KafkaProperties kafkaProperties =
        kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
    return new ClusterAssignmentCache(
        curator,
        kafkaProperties.getClusterName(),
        zookeeperPaths,
        subscriptionIds,
        consumerNodesRegistry);
  }

  @Bean(initMethod = "start", destroyMethod = "stop")
  public ConsumerAssignmentCache consumerAssignmentCache(
      CuratorFramework curator,
      WorkloadProperties workloadProperties,
      KafkaClustersProperties kafkaClustersProperties,
      ZookeeperPaths zookeeperPaths,
      SubscriptionIds subscriptionIds,
      DatacenterNameProvider datacenterNameProvider) {
    KafkaProperties kafkaProperties =
        kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
    return new ConsumerAssignmentCache(
        curator,
        workloadProperties.getNodeId(),
        kafkaProperties.getClusterName(),
        zookeeperPaths,
        subscriptionIds);
  }
}
