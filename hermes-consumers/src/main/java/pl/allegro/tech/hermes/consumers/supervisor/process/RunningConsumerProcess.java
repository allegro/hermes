package pl.allegro.tech.hermes.consumers.supervisor.process;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;

class RunningConsumerProcess {
  private static final Logger logger = LoggerFactory.getLogger(RunningConsumerProcess.class);

  private ConsumerProcess process;
  private Future executionHandle;
  private Clock clock;
  private Optional<Long> timeOfDeath = Optional.empty();

  RunningConsumerProcess(ConsumerProcess process, Future executionHandle, Clock clock) {
    this.process = process;
    this.executionHandle = executionHandle;
    this.clock = clock;
  }

  private RunningConsumerProcess(
      ConsumerProcess process, Future executionHandle, long killTime, Clock clock) {
    this(process, executionHandle, clock);
    this.timeOfDeath = Optional.of(killTime);
  }

  void cancel() {
    timeOfDeath.ifPresent(
        timeOfDeath -> {
          SubscriptionName subscriptionName = process.getSubscriptionName();
          logger.info(
              "Canceling consumer process for subscription {}. It should have been killed {}ms ago.",
              subscriptionName,
              clock.millis() - timeOfDeath);
          if (executionHandle.cancel(true)) {
            logger.info("Canceled consumer process for subscription {}", subscriptionName);
          } else {
            logger.error("Failed to cancel consumer process for {}.", subscriptionName);
          }
        });
  }

  boolean shouldBeCanceledNow() {
    return timeOfDeath
        .map(time -> time < clock.millis() && !executionHandle.isDone())
        .orElse(false);
  }

  RunningConsumerProcess copyWithTimeOfDeath(long timeOfDeath) {
    return new RunningConsumerProcess(process, executionHandle, timeOfDeath, clock);
  }

  ConsumerProcess getConsumerProcess() {
    return process;
  }

  public Subscription getSubscription() {
    return getConsumerProcess().getSubscription();
  }
}
