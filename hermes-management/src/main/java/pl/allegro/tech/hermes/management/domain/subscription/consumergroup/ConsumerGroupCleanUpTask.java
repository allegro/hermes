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

public class ConsumerGroupCleanUpTask implements Runnable {
  private final Logger logger = LoggerFactory.getLogger(ConsumerGroupCleanUpTask.class);

  private final MultiDCAwareService multiDCAwareService;
  private final Map<String, ConsumerGroupToDeleteRepository>
      consumerGroupToDeleteRepositoriesByDatacenter;
  private final SubscriptionService subscriptionService;
  private final Clock clock;

  private final Duration cleanUpInitialDelay;
  private final Duration cleanUpTimeout;

  public ConsumerGroupCleanUpTask(
      MultiDCAwareService multiDCAwareService,
      Map<String, ConsumerGroupToDeleteRepository> consumerGroupToDeleteRepositoriesByDatacenter,
      SubscriptionService subscriptionService,
      ConsumerGroupCleanUpProperties cleanUpProperties,
      Clock clock) {
    this.multiDCAwareService = multiDCAwareService;
    this.consumerGroupToDeleteRepositoriesByDatacenter =
        consumerGroupToDeleteRepositoriesByDatacenter;
    this.subscriptionService = subscriptionService;
    this.clock = clock;
    this.cleanUpInitialDelay = cleanUpProperties.getInitialDelay();
    this.cleanUpTimeout = cleanUpProperties.getTimeout();
  }

  @Override
  public void run() {
    consumerGroupToDeleteRepositoriesByDatacenter.values().stream()
        .flatMap(repository -> repository.getAllConsumerGroupsToDelete().stream())
        .filter(this::shouldConsumerGroupDeletionTaskBeProcessed)
        .forEach(this::tryToDeleteConsumerGroup);
  }

  private void tryToDeleteConsumerGroup(ConsumerGroupToDelete consumerGroupToDelete) {
    if (subscriptionService.subscriptionExists(consumerGroupToDelete.subscriptionName())) {
      logger.info(
          "Subscription {} still exists, skipping deletion of consumer group in datacenter {}",
          consumerGroupToDelete.subscriptionName().getQualifiedName(),
          consumerGroupToDelete.datacenter());
    } else {
      logger.info(
          "Deleting consumer group for subscription {} in datacenter {}",
          consumerGroupToDelete.subscriptionName().getQualifiedName(),
          consumerGroupToDelete.datacenter());

      try {
        multiDCAwareService.deleteConsumerGroupForDatacenter(
            consumerGroupToDelete.subscriptionName(), consumerGroupToDelete.datacenter());
      } catch (Exception e) {
        logger.error(
            "Failed to delete consumer group for subscription {} in datacenter {}",
            consumerGroupToDelete.subscriptionName().getQualifiedName(),
            consumerGroupToDelete.datacenter(),
            e);
        return;
      }

      logger.info(
          "Successfully deleted consumer group for subscription {} in datacenter {}",
          consumerGroupToDelete.subscriptionName().getQualifiedName(),
          consumerGroupToDelete.datacenter());
    }

    consumerGroupToDeleteRepositoriesByDatacenter
        .get(consumerGroupToDelete.datacenter())
        .deleteConsumerGroupToDeleteTask(consumerGroupToDelete);

    logger.info(
        "Deleted consumer group deletion task for subscription {} in datacenter {}",
        consumerGroupToDelete.subscriptionName().getQualifiedName(),
        consumerGroupToDelete.datacenter());
  }

  private boolean shouldConsumerGroupDeletionTaskBeProcessed(
      ConsumerGroupToDelete consumerGroupToDelete) {
    Duration taskAge = Duration.between(consumerGroupToDelete.requestedAt(), Instant.now(clock));
    return taskAge.compareTo(cleanUpInitialDelay) >= 0 && taskAge.compareTo(cleanUpTimeout) <= 0;
  }
}
