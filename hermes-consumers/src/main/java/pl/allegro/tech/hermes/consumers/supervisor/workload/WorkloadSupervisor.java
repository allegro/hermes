package pl.allegro.tech.hermes.consumers.supervisor.workload;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.admin.AdminOperationsCallback;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.SubscriptionCallback;
import pl.allegro.tech.hermes.domain.notifications.TopicCallback;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;

public class WorkloadSupervisor
    implements SubscriptionCallback,
        TopicCallback,
        SubscriptionAssignmentAware,
        AdminOperationsCallback {

  private static final Logger logger = LoggerFactory.getLogger(WorkloadSupervisor.class);

  private final ConsumersSupervisor supervisor;
  private final InternalNotificationsBus notificationsBus;
  private final SubscriptionsCache subscriptionsCache;
  private final ConsumerAssignmentCache assignmentCache;
  private final ConsumerNodesRegistry consumersRegistry;
  private final BalancingJob balancingJob;
  private final ZookeeperAdminCache adminCache;
  private final WorkBalancingParameters workBalancingParameters;
  private final ExecutorService assignmentExecutor;
  private final ScheduledExecutorService rebalanceScheduler;

  public WorkloadSupervisor(
      ConsumersSupervisor supervisor,
      InternalNotificationsBus notificationsBus,
      SubscriptionsCache subscriptionsCache,
      ConsumerAssignmentCache assignmentCache,
      ConsumerAssignmentRegistry consumerAssignmentRegistry,
      ClusterAssignmentCache clusterAssignmentCache,
      ConsumerNodesRegistry consumersRegistry,
      ZookeeperAdminCache adminCache,
      ExecutorService assignmentExecutor,
      WorkBalancingParameters workBalancingParameters,
      String kafkaClusterName,
      MetricsFacade metrics,
      WorkloadConstraintsRepository workloadConstraintsRepository,
      WorkBalancer workBalancer,
      BalancingListener balancingListener) {
    this.supervisor = supervisor;
    this.notificationsBus = notificationsBus;
    this.subscriptionsCache = subscriptionsCache;
    this.assignmentCache = assignmentCache;
    this.consumersRegistry = consumersRegistry;
    this.adminCache = adminCache;
    this.assignmentExecutor = assignmentExecutor;
    this.workBalancingParameters = workBalancingParameters;
    this.balancingJob =
        new BalancingJob(
            consumersRegistry,
            workBalancingParameters,
            subscriptionsCache,
            clusterAssignmentCache,
            consumerAssignmentRegistry,
            workBalancer,
            metrics,
            kafkaClusterName,
            workloadConstraintsRepository,
            balancingListener);
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("balancing-executor-%d").build();
    this.rebalanceScheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
  }

  @Override
  public void onSubscriptionAssigned(SubscriptionName subscriptionName) {
    Subscription subscription = subscriptionsCache.getSubscription(subscriptionName);
    logger.info("Scheduling assignment consumer for {}", subscription.getQualifiedName());
    assignmentExecutor.execute(
        () -> {
          logger.info("Assigning consumer for {}", subscription.getQualifiedName());
          supervisor.assignConsumerForSubscription(subscription);
          logger.info("Consumer assigned for {}", subscription.getQualifiedName());
        });
  }

  @Override
  public void onAssignmentRemoved(SubscriptionName subscription) {
    logger.info("Scheduling assignment removal consumer for {}", subscription.getQualifiedName());
    assignmentExecutor.execute(
        () -> {
          logger.info("Removing assignment from consumer for {}", subscription.getQualifiedName());
          supervisor.deleteConsumerForSubscriptionName(subscription);
          logger.info("Consumer removed for {}", subscription.getName());
        });
  }

  @Override
  public void onSubscriptionChanged(Subscription subscription) {
    if (assignmentCache.isAssignedTo(subscription.getQualifiedName())) {
      logger.info("Updating subscription {}", subscription.getName());
      supervisor.updateSubscription(subscription);
    }
  }

  @Override
  public void onTopicChanged(Topic topic) {
    for (Subscription subscription : subscriptionsCache.subscriptionsOfTopic(topic.getName())) {
      if (assignmentCache.isAssignedTo(subscription.getQualifiedName())) {
        supervisor.updateTopic(subscription, topic);
      }
    }
  }

  public void start() throws Exception {
    final long startTime = System.currentTimeMillis();

    adminCache.start();
    adminCache.addCallback(this);

    notificationsBus.registerSubscriptionCallback(this);
    notificationsBus.registerTopicCallback(this);
    assignmentCache.registerAssignmentCallback(this);

    supervisor.start();
    if (workBalancingParameters.isAutoRebalance()) {
      rebalanceScheduler.scheduleWithFixedDelay(
          balancingJob,
          workBalancingParameters.getRebalanceInterval().toMillis(),
          workBalancingParameters.getRebalanceInterval().toMillis(),
          MILLISECONDS);
    } else {
      logger.info("Automatic workload rebalancing is disabled.");
    }

    logger.info("Consumer boot complete in {} ms.", System.currentTimeMillis() - startTime);
  }

  public Set<SubscriptionName> assignedSubscriptions() {
    return assignmentCache.getConsumerSubscriptions();
  }

  public void shutdown() throws Exception {
    rebalanceScheduler.shutdown();
    rebalanceScheduler.awaitTermination(1, TimeUnit.MINUTES);
    supervisor.shutdown();
  }

  @Override
  public Optional<String> watchedConsumerId() {
    return Optional.of(consumersRegistry.getConsumerId());
  }

  public String consumerId() {
    return consumersRegistry.getConsumerId();
  }

  public boolean isLeader() {
    return consumersRegistry.isLeader();
  }

  @Override
  public void onRetransmissionStarts(SubscriptionName subscription) throws Exception {
    if (assignmentCache.isAssignedTo(subscription)) {
      logger.info("Triggering retransmission for subscription {}", subscription);
      supervisor.retransmit(subscription);
    }
  }
}
