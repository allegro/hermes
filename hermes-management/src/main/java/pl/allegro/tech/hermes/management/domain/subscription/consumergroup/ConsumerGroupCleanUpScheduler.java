package pl.allegro.tech.hermes.management.domain.subscription.consumergroup;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.management.config.subscription.consumergroup.ConsumerGroupCleanUpProperties;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;
import pl.allegro.tech.hermes.management.infrastructure.leader.ManagementLeadership;

public class ConsumerGroupCleanUpScheduler {
  private final Logger logger = LoggerFactory.getLogger(ConsumerGroupCleanUpScheduler.class);

  private final MultiDCAwareService multiDCAwareService;
  private final Map<String, ConsumerGroupToDeleteRepository>
      consumerGroupToDeleteRepositoriesByDatacenter;
  private final SubscriptionService subscriptionService;
  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder().setNameFormat("consumer-group-clean-up-%d").build());
  private final ManagementLeadership managementLeadership;
  private final Clock clock;
  private final boolean enabled;
  private final long cleanUpIntervalInSeconds;
  private final ConsumerGroupCleanUpProperties cleanUpProperties;

  public ConsumerGroupCleanUpScheduler(
      MultiDCAwareService multiDCAwareService,
      Map<String, ConsumerGroupToDeleteRepository> consumerGroupToDeleteRepositoriesByDatacenter,
      SubscriptionService subscriptionService,
      ConsumerGroupCleanUpProperties cleanUpProperties,
      ManagementLeadership managementLeadership,
      Clock clock) {
    this.multiDCAwareService = multiDCAwareService;
    this.consumerGroupToDeleteRepositoriesByDatacenter =
        consumerGroupToDeleteRepositoriesByDatacenter;
    this.subscriptionService = subscriptionService;
    this.managementLeadership = managementLeadership;
    this.clock = clock;
    this.enabled = cleanUpProperties.isEnabled();
    this.cleanUpIntervalInSeconds = cleanUpProperties.getInterval().toSeconds();
    this.cleanUpProperties = cleanUpProperties;
  }

  @PostConstruct
  public void start() {
    if (enabled) {
      logger.info("Starting the consumer group clean up task");
      ConsumerGroupCleanUpTask consumerGroupCleanUpTask =
          new ConsumerGroupCleanUpTask(
              multiDCAwareService,
              consumerGroupToDeleteRepositoriesByDatacenter,
              subscriptionService,
              cleanUpProperties,
              managementLeadership,
              clock);
      scheduler.scheduleAtFixedRate(
          consumerGroupCleanUpTask, 0, cleanUpIntervalInSeconds, TimeUnit.SECONDS);
    } else {
      logger.info("Consumer group clean up is disabled");
    }
  }

  @PreDestroy
  public void stop() {
    scheduler.shutdown();
  }
}
