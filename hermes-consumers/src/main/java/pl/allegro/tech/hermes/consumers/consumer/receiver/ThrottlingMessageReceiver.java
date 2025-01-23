package pl.allegro.tech.hermes.consumers.consumer.receiver;

import java.util.Optional;
import java.util.Set;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.idletime.IdleTimeCalculator;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;

public class ThrottlingMessageReceiver implements MessageReceiver {

  private final MessageReceiver receiver;
  private final IdleTimeCalculator idleTimeCalculator;
  private final HermesTimer idleTimer;

  public ThrottlingMessageReceiver(
      MessageReceiver receiver,
      IdleTimeCalculator idleTimeCalculator,
      SubscriptionName subscriptionName,
      MetricsFacade metrics) {
    this.receiver = receiver;
    this.idleTimeCalculator = idleTimeCalculator;
    this.idleTimer = metrics.subscriptions().consumerIdleTimer(subscriptionName);
  }

  @Override
  public Optional<Message> next() {
    Optional<Message> next = receiver.next();
    if (next.isPresent()) {
      idleTimeCalculator.reset();
    } else {
      awaitUntilNextPoll();
    }
    return next;
  }

  private void awaitUntilNextPoll() {
    try (HermesTimerContext ignored = idleTimer.time()) {
      Thread.sleep(idleTimeCalculator.increaseIdleTime());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void commit(Set<SubscriptionPartitionOffset> offsets) {
    receiver.commit(offsets);
  }

  @Override
  public PartitionOffsets moveOffset(PartitionOffsets offsets) {
    return receiver.moveOffset(offsets);
  }

  @Override
  public Set<Integer> getAssignedPartitions() {
    return receiver.getAssignedPartitions();
  }

  @Override
  public void stop() {
    receiver.stop();
  }

  @Override
  public void update(Subscription newSubscription) {
    this.receiver.update(newSubscription);
  }
}
