package pl.allegro.tech.hermes.consumers.supervisor.workload;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.domain.workload.constraints.ConsumersWorkloadConstraints;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;

class BalancingJob implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(BalancingJob.class);

  private final ConsumerNodesRegistry consumersRegistry;
  private final WorkBalancingParameters workBalancingParameters;
  private final SubscriptionsCache subscriptionsCache;
  private final ClusterAssignmentCache clusterAssignmentCache;
  private final ConsumerAssignmentRegistry consumerAssignmentRegistry;
  private final WorkBalancer workBalancer;
  private final MetricsFacade metrics;
  private final String kafkaCluster;
  private final WorkloadConstraintsRepository workloadConstraintsRepository;
  private final BalancingListener balancingListener;
  private final BalancingJobMetrics balancingMetrics = new BalancingJobMetrics();

  BalancingJob(
      ConsumerNodesRegistry consumersRegistry,
      WorkBalancingParameters workBalancingParameters,
      SubscriptionsCache subscriptionsCache,
      ClusterAssignmentCache clusterAssignmentCache,
      ConsumerAssignmentRegistry consumerAssignmentRegistry,
      WorkBalancer workBalancer,
      MetricsFacade metrics,
      String kafkaCluster,
      WorkloadConstraintsRepository workloadConstraintsRepository,
      BalancingListener balancingListener) {
    this.consumersRegistry = consumersRegistry;
    this.workBalancingParameters = workBalancingParameters;
    this.subscriptionsCache = subscriptionsCache;
    this.clusterAssignmentCache = clusterAssignmentCache;
    this.consumerAssignmentRegistry = consumerAssignmentRegistry;
    this.workBalancer = workBalancer;
    this.metrics = metrics;
    this.kafkaCluster = kafkaCluster;
    this.workloadConstraintsRepository = workloadConstraintsRepository;
    this.balancingListener = balancingListener;
    metrics
        .workload()
        .registerAllAssignmentsGauge(balancingMetrics, kafkaCluster, bm -> bm.allAssignments);
    metrics
        .workload()
        .registerMissingResourcesGauge(balancingMetrics, kafkaCluster, bm -> bm.missingResources);
    metrics
        .workload()
        .registerDeletedAssignmentsGauge(
            balancingMetrics, kafkaCluster, bm -> bm.deletedAssignments);
    metrics
        .workload()
        .registerCreatedAssignmentsGauge(
            balancingMetrics, kafkaCluster, bm -> bm.createdAssignments);
  }

  @Override
  public void run() {
    try {
      consumersRegistry.refresh();
      if (consumersRegistry.isLeader()) {
        try (HermesTimerContext ignored =
            metrics.workload().rebalanceDurationTimer(kafkaCluster).time()) {
          logger.info("Initializing workload balance.");
          clusterAssignmentCache.refresh();

          SubscriptionAssignmentView initialState = clusterAssignmentCache.createSnapshot();
          List<String> activeConsumers = consumersRegistry.listConsumerNodes();
          List<SubscriptionName> activeSubscriptions =
              subscriptionsCache.listActiveSubscriptionNames();

          balancingListener.onBeforeBalancing(activeConsumers);

          WorkBalancingResult work =
              workBalancer.balance(
                  activeSubscriptions,
                  activeConsumers,
                  initialState,
                  prepareWorkloadConstraints(activeConsumers));

          if (consumersRegistry.isLeader()) {
            logger.info("Applying workload balance changes");
            WorkDistributionChanges changes = calculateWorkDistributionChanges(initialState, work);
            applyWorkloadChanges(changes, work);
            logger.info("Finished workload balance");

            clusterAssignmentCache.refresh(); // refresh cache with just stored data

            balancingListener.onAfterBalancing(changes);

            updateMetrics(work, changes);
          } else {
            logger.info("Lost leadership before applying changes");
          }
        }
      } else {
        balancingMetrics.reset();
        balancingListener.onBalancingSkipped();
      }
    } catch (Exception e) {
      logger.error("Caught exception when running balancing job", e);
    }
  }

  private WorkloadConstraints prepareWorkloadConstraints(List<String> activeConsumers) {
    ConsumersWorkloadConstraints constraints =
        workloadConstraintsRepository.getConsumersWorkloadConstraints();
    return WorkloadConstraints.builder()
        .withActiveConsumers(activeConsumers.size())
        .withConsumersPerSubscription(workBalancingParameters.getConsumersPerSubscription())
        .withMaxSubscriptionsPerConsumer(workBalancingParameters.getMaxSubscriptionsPerConsumer())
        .withSubscriptionConstraints(constraints.getSubscriptionConstraints())
        .withTopicConstraints(constraints.getTopicConstraints())
        .build();
  }

  private WorkDistributionChanges calculateWorkDistributionChanges(
      SubscriptionAssignmentView initialState, WorkBalancingResult workBalancingResult) {
    SubscriptionAssignmentView balancedState = workBalancingResult.getAssignmentsView();
    SubscriptionAssignmentView deletions = initialState.deletions(balancedState);
    SubscriptionAssignmentView additions = initialState.additions(balancedState);
    return new WorkDistributionChanges(deletions, additions);
  }

  private void applyWorkloadChanges(
      WorkDistributionChanges changes, WorkBalancingResult workBalancingResult) {
    SubscriptionAssignmentView balancedState = workBalancingResult.getAssignmentsView();
    for (String consumerId : changes.getModifiedConsumerNodes()) {
      consumerAssignmentRegistry.updateAssignments(
          consumerId, balancedState.getSubscriptionsForConsumerNode(consumerId));
    }
  }

  private void updateMetrics(WorkBalancingResult balancingResult, WorkDistributionChanges changes) {
    this.balancingMetrics.allAssignments =
        balancingResult.getAssignmentsView().getAllAssignments().size();
    this.balancingMetrics.missingResources = balancingResult.getMissingResources();
    this.balancingMetrics.createdAssignments = changes.getCreatedAssignmentsCount();
    this.balancingMetrics.deletedAssignments = changes.getDeletedAssignmentsCount();
  }

  private static class BalancingJobMetrics {

    volatile int allAssignments;

    volatile int missingResources;

    volatile int deletedAssignments;

    volatile int createdAssignments;

    void reset() {
      this.allAssignments = 0;
      this.missingResources = 0;
      this.deletedAssignments = 0;
      this.createdAssignments = 0;
    }
  }
}
