package pl.allegro.tech.hermes.management.domain.subscription.consumergroup;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.management.config.subscription.consumergroup.ConsumerGroupCleanUpProperties;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;
import pl.allegro.tech.hermes.management.infrastructure.leader.ManagementLeadership;

public class ConsumerGroupCleanUpTask implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ConsumerGroupCleanUpTask.class);

  private final MultiDCAwareService multiDCAwareService;
  private final Map<String, ConsumerGroupToDeleteRepository> repositoriesByDatacenter;
  private final SubscriptionService subscriptionService;
  private final ManagementLeadership managementLeadership;
  private final Clock clock;

  private final Duration initialDelay;
  private final Duration timeout;
  private final boolean removeTasksAfterTimeout;

  public ConsumerGroupCleanUpTask(
      MultiDCAwareService multiDCAwareService,
      Map<String, ConsumerGroupToDeleteRepository> repositoriesByDatacenter,
      SubscriptionService subscriptionService,
      ConsumerGroupCleanUpProperties cleanUpProperties,
      ManagementLeadership managementLeadership,
      Clock clock) {

    this.multiDCAwareService = multiDCAwareService;
    this.repositoriesByDatacenter = repositoriesByDatacenter;
    this.subscriptionService = subscriptionService;
    this.managementLeadership = managementLeadership;
    this.clock = clock;

    this.initialDelay = cleanUpProperties.getInitialDelay();
    this.timeout = cleanUpProperties.getTimeout();
    this.removeTasksAfterTimeout = cleanUpProperties.isRemoveTasksAfterTimeout();
  }

  @Override
  public void run() {
    if (managementLeadership.isLeader()) {
      repositoriesByDatacenter.values().stream()
          .flatMap(repository -> repository.getAllConsumerGroupsToDelete().stream())
          .filter(this::isTaskReadyForProcessing)
          .forEach(this::processDeletionTask);
    }
  }

  private void processDeletionTask(ConsumerGroupToDelete task) {
    if (subscriptionService.subscriptionExists(task.subscriptionName())) {
      logSkippingDeletion(task);
      removeDeletionTask(task);
      return;
    }

    if (isTaskExpired(task)) {
      logTaskExpiration(task);
      if (removeTasksAfterTimeout) {
        removeDeletionTask(task);
      }
      return;
    }

    if (deleteConsumerGroup(task)) {
      removeDeletionTask(task);
    }
  }

  private boolean deleteConsumerGroup(ConsumerGroupToDelete task) {
    logDeletionAttempt(task);
    try {
      multiDCAwareService.deleteConsumerGroupForDatacenter(
          task.subscriptionName(), task.datacenter());
    } catch (Exception e) {
      logDeletionFailure(task, e);
      return false;
    }
    logSuccessfulDeletion(task);
    return true;
  }

  private void removeDeletionTask(ConsumerGroupToDelete task) {
    repositoriesByDatacenter.get(task.datacenter()).deleteConsumerGroupToDeleteTask(task);
    logTaskRemoval(task);
  }

  private boolean isTaskReadyForProcessing(ConsumerGroupToDelete task) {
    Duration taskAge = Duration.between(task.requestedAt(), Instant.now(clock));
    return !taskAge.minus(initialDelay).isNegative();
  }

  private boolean isTaskExpired(ConsumerGroupToDelete task) {
    Duration taskAge = Duration.between(task.requestedAt(), Instant.now(clock));
    return taskAge.compareTo(initialDelay.plus(timeout)) > 0;
  }

  private void logSkippingDeletion(ConsumerGroupToDelete task) {
    logger.info(
        "Skipping consumer group deletion: Subscription {} still exists in datacenter {}",
        task.subscriptionName().getQualifiedName(),
        task.datacenter());
  }

  private void logTaskExpiration(ConsumerGroupToDelete task) {
    logger.warn(
        "Consumer group deletion task expired: Subscription {} in datacenter {}",
        task.subscriptionName().getQualifiedName(),
        task.datacenter());
  }

  private void logDeletionAttempt(ConsumerGroupToDelete task) {
    logger.info(
        "Attempting to delete consumer group for subscription {} in datacenter {}",
        task.subscriptionName().getQualifiedName(),
        task.datacenter());
  }

  private void logDeletionFailure(ConsumerGroupToDelete task, Exception e) {
    logger.error(
        "Failed to delete consumer group for subscription {} in datacenter {}",
        task.subscriptionName().getQualifiedName(),
        task.datacenter(),
        e);
  }

  private void logSuccessfulDeletion(ConsumerGroupToDelete task) {
    logger.info(
        "Successfully deleted consumer group for subscription {} in datacenter {}",
        task.subscriptionName().getQualifiedName(),
        task.datacenter());
  }

  private void logTaskRemoval(ConsumerGroupToDelete task) {
    logger.info(
        "Removed consumer group deletion task for subscription {} in datacenter {}",
        task.subscriptionName().getQualifiedName(),
        task.datacenter());
  }
}
