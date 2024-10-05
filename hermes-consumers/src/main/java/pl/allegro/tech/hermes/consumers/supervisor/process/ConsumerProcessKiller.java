package pl.allegro.tech.hermes.consumers.supervisor.process;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;

class ConsumerProcessKiller {
  private static final Logger logger = LoggerFactory.getLogger(ConsumerProcessKiller.class);

  private final long killAfterMs;
  private final Clock clock;
  private final RunningConsumerProcesses dyingConsumerProcesses;

  ConsumerProcessKiller(long killAfterMs, Clock clock) {
    this.clock = clock;
    this.killAfterMs = killAfterMs;
    this.dyingConsumerProcesses = new RunningConsumerProcesses(clock);
  }

  int countDying() {
    return dyingConsumerProcesses.count();
  }

  void killAllDying() {
    dyingConsumerProcesses.stream()
        .filter(RunningConsumerProcess::shouldBeCanceledNow)
        .forEach(RunningConsumerProcess::cancel);
  }

  void kill(RunningConsumerProcess process) {
    observe(process).cancel();
  }

  boolean isDying(SubscriptionName subscriptionName) {
    return dyingConsumerProcesses.hasProcess(subscriptionName);
  }

  void cleanup(SubscriptionName subscriptionName) {
    logger.info("Removing consumer process for subscription {}", subscriptionName);
    dyingConsumerProcesses.remove(subscriptionName);
  }

  RunningConsumerProcess observe(RunningConsumerProcess process) {
    RunningConsumerProcess observed = process.copyWithTimeOfDeath(clock.millis() + killAfterMs);
    dyingConsumerProcesses.add(observed);
    return observed;
  }
}
