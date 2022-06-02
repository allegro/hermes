package pl.allegro.tech.hermes.consumers.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerAuthorizationHandler;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerMessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchFactory;
import pl.allegro.tech.hermes.consumers.consumer.converter.MessageConverterResolver;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
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
import pl.allegro.tech.hermes.consumers.supervisor.workload.ClusterAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorController;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.SelectiveSupervisorController;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.slf4j.LoggerFactory.getLogger;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_ASSIGNMENT_PROCESSING_THREAD_POOL_SIZE;

@Configuration
@EnableConfigurationProperties({
        CommitOffsetProperties.class
})
public class SupervisorConfiguration {
    private static final Logger logger = getLogger(SupervisorConfiguration.class);

    @Bean(initMethod = "start", destroyMethod = "shutdown")
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
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("AssignmentExecutor-%d")
                .setUncaughtExceptionHandler((t, e) -> logger.error("AssignmentExecutor failed {}", t.getName(), e)).build();
        ExecutorService assignmentExecutor = newFixedThreadPool(configs.getIntProperty(CONSUMER_WORKLOAD_ASSIGNMENT_PROCESSING_THREAD_POOL_SIZE), threadFactory);
        return new SelectiveSupervisorController(
                supervisor,
                notificationsBus,
                subscriptionsCache,
                consumerAssignmentCache,
                assignmentRegistry,
                clusterAssignmentCache,
                consumerNodesRegistry,
                adminCache,
                assignmentExecutor,
                configs,
                metrics,
                workloadConstraintsRepository
        );
    }

    @Bean
    public Retransmitter retransmitter(SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator,
                                       ConfigFactory configs) {
        return new Retransmitter(subscriptionOffsetChangeIndicator, configs);
    }

    @Bean
    public ConsumerFactory consumerFactory(ReceiverFactory messageReceiverFactory,
                                           HermesMetrics hermesMetrics,
                                           ConfigFactory configFactory,
                                           ConsumerRateLimitSupervisor consumerRateLimitSupervisor,
                                           OutputRateCalculatorFactory outputRateCalculatorFactory,
                                           Trackers trackers,
                                           OffsetQueue offsetQueue,
                                           ConsumerMessageSenderFactory consumerMessageSenderFactory,
                                           TopicRepository topicRepository,
                                           MessageConverterResolver messageConverterResolver,
                                           MessageBatchFactory byteBufferMessageBatchFactory,
                                           CompositeMessageContentWrapper compositeMessageContentWrapper,
                                           MessageBatchSenderFactory batchSenderFactory,
                                           ConsumerAuthorizationHandler consumerAuthorizationHandler,
                                           Clock clock) {
        return new ConsumerFactory(
                messageReceiverFactory,
                hermesMetrics,
                configFactory,
                consumerRateLimitSupervisor,
                outputRateCalculatorFactory,
                trackers,
                offsetQueue,
                consumerMessageSenderFactory,
                topicRepository,
                messageConverterResolver,
                byteBufferMessageBatchFactory,
                compositeMessageContentWrapper,
                batchSenderFactory,
                consumerAuthorizationHandler,
                clock
        );
    }

    @Bean
    public ConsumersExecutorService consumersExecutorService(ConfigFactory configFactory,
                                                             HermesMetrics hermesMetrics) {
        return new ConsumersExecutorService(configFactory, hermesMetrics);
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
                                                              Clock clock,
                                                              CommitOffsetProperties commitOffsetProperties) {
        return new NonblockingConsumersSupervisor(configFactory, executor, consumerFactory, offsetQueue,
                consumerPartitionAssignmentState, retransmitter, undeliveredMessageLogPersister,
                subscriptionRepository, metrics, monitor, clock, commitOffsetProperties.getPeriod());
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public ConsumersRuntimeMonitor consumersRuntimeMonitor(ConsumersSupervisor consumerSupervisor,
                                                           SupervisorController workloadSupervisor,
                                                           HermesMetrics hermesMetrics,
                                                           SubscriptionsCache subscriptionsCache,
                                                           ConfigFactory configFactory) {
        return new ConsumersRuntimeMonitor(
                consumerSupervisor,
                workloadSupervisor,
                hermesMetrics,
                subscriptionsCache,
                configFactory
        );
    }

    @Bean
    public ConsumerAssignmentRegistry consumerAssignmentRegistry(CuratorFramework curator,
                                                                 ConfigFactory configFactory,
                                                                 ZookeeperPaths zookeeperPaths,
                                                                 SubscriptionIds subscriptionIds) {
        return new ConsumerAssignmentRegistry(curator, configFactory, zookeeperPaths, subscriptionIds);
    }

    @Bean
    public ClusterAssignmentCache clusterAssignmentCache(CuratorFramework curator,
                                                         ConfigFactory configFactory,
                                                         ZookeeperPaths zookeeperPaths,
                                                         SubscriptionIds subscriptionIds,
                                                         ConsumerNodesRegistry consumerNodesRegistry) {
        String clusterName = configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME);
        return new ClusterAssignmentCache(curator, clusterName, zookeeperPaths, subscriptionIds, consumerNodesRegistry);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public ConsumerAssignmentCache consumerAssignmentCache(CuratorFramework curator,
                                                           ConfigFactory configFactory,
                                                           ZookeeperPaths zookeeperPaths,
                                                           SubscriptionIds subscriptionIds) {
        String consumerId = configFactory.getStringProperty(Configs.CONSUMER_WORKLOAD_NODE_ID);
        String clusterName = configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME);
        return new ConsumerAssignmentCache(curator, consumerId, clusterName, zookeeperPaths, subscriptionIds);
    }
}
