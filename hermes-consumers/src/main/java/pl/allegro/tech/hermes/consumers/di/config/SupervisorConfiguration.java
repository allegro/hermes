package pl.allegro.tech.hermes.consumers.di.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
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
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerWorkloadRegistryType;
import pl.allegro.tech.hermes.consumers.supervisor.workload.FlatBinaryClusterAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.FlatBinaryConsumerAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.FlatBinaryConsumerAssignmentRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.workload.HierarchicalConsumerAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.HierarchicalConsumerAssignmentRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentPathSerializer;
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
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_CLUSTER_NAME;
import static pl.allegro.tech.hermes.consumers.supervisor.workload.HierarchicalConsumerAssignmentRegistry.AUTO_ASSIGNED_MARKER;

@Configuration
public class SupervisorConfiguration {
    private static final Logger logger = getLogger(SupervisorConfiguration.class);

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
                messageContentWrapper,
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
                                                              Clock clock) {
        return new NonblockingConsumersSupervisor(configFactory, executor, consumerFactory, offsetQueue,
                consumerPartitionAssignmentState, retransmitter, undeliveredMessageLogPersister,
                subscriptionRepository, metrics, monitor, clock);
    }

    @Bean(destroyMethod = "shutdown")
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
                                                                 ConsumerAssignmentCache consumerAssignmentCache,
                                                                 SubscriptionIds subscriptionIds) {
        ConsumerWorkloadRegistryType type;
        try {
            String typeString = configFactory.getStringProperty(Configs.CONSUMER_WORKLOAD_REGISTRY_TYPE);
            type = ConsumerWorkloadRegistryType.fromString(typeString);
        } catch (Exception e) {
            logger.error("Could not configure consumer workload registry", e);
            throw e;
        }
        logger.info("Consumer workload registry type chosen: {}", type.getConfigValue());

        switch (type) {
            case HIERARCHICAL:
                String cluster = configFactory.getStringProperty(KAFKA_CLUSTER_NAME);
                String consumersRuntimePath = zookeeperPaths.consumersRuntimePath(cluster);
                SubscriptionAssignmentPathSerializer pathSerializer = new SubscriptionAssignmentPathSerializer(consumersRuntimePath, AUTO_ASSIGNED_MARKER);
                CreateMode assignmentNodeCreationMode = CreateMode.PERSISTENT;
                return new HierarchicalConsumerAssignmentRegistry(
                        curator,
                        consumerAssignmentCache,
                        pathSerializer,
                        assignmentNodeCreationMode
                );
            case FLAT_BINARY:
                return new FlatBinaryConsumerAssignmentRegistry(curator, configFactory, zookeeperPaths, subscriptionIds);
            default:
                throw new UnsupportedOperationException("Max-rate type not supported.");
        }
    }

    @Bean
    public ClusterAssignmentCache clusterAssignmentCache(CuratorFramework curator,
                                                         ConfigFactory configFactory,
                                                         ConsumerAssignmentCache consumerAssignmentCache,
                                                         ZookeeperPaths zookeeperPaths,
                                                         SubscriptionIds subscriptionIds,
                                                         ConsumerNodesRegistry consumerNodesRegistry) {
        ConsumerWorkloadRegistryType type;
        try {
            String typeString = configFactory.getStringProperty(Configs.CONSUMER_WORKLOAD_REGISTRY_TYPE);
            type = ConsumerWorkloadRegistryType.fromString(typeString);
        } catch (Exception e) {
            logger.error("Could not configure subscription assignment notifying repository based on specified consumer workload registry type", e);
            throw e;
        }

        String clusterName = configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME);

        switch (type) {
            case HIERARCHICAL:
                if (consumerAssignmentCache instanceof HierarchicalConsumerAssignmentCache) {
                    return (HierarchicalConsumerAssignmentCache) consumerAssignmentCache;
                } else {
                    throw new IllegalStateException("Invalid type of HierarchicalConsumerAssignmentCache was registered for this type of workload registry");
                }
            case FLAT_BINARY:
                return new FlatBinaryClusterAssignmentCache(curator, clusterName, zookeeperPaths, subscriptionIds, consumerNodesRegistry);
            default:
                throw new UnsupportedOperationException("Workload registry type not supported.");
        }
    }

    @Bean
    public ConsumerAssignmentCache consumerAssignmentCache(CuratorFramework curator,
                                                           ConfigFactory configFactory,
                                                           ZookeeperPaths zookeeperPaths,
                                                           SubscriptionsCache subscriptionsCache,
                                                           SubscriptionIds subscriptionIds) {
        ConsumerWorkloadRegistryType type;
        try {
            String typeString = configFactory.getStringProperty(Configs.CONSUMER_WORKLOAD_REGISTRY_TYPE);
            type = ConsumerWorkloadRegistryType.fromString(typeString);
        } catch (Exception e) {
            logger.error("Could not configure consumer assignment notifying cache based on specified consumer workload registry type", e);
            throw e;
        }

        String consumerId = configFactory.getStringProperty(Configs.CONSUMER_WORKLOAD_NODE_ID);
        String clusterName = configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME);

        switch (type) {
            case HIERARCHICAL:
                return new HierarchicalConsumerAssignmentCache(curator, consumerId, clusterName, zookeeperPaths, subscriptionsCache);
            case FLAT_BINARY:
                return new FlatBinaryConsumerAssignmentCache(curator, consumerId, clusterName, zookeeperPaths, subscriptionIds);
            default:
                throw new UnsupportedOperationException("Workload registry type not supported.");
        }
    }
}
