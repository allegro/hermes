package pl.allegro.tech.hermes.benchmark.consumer;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.consumer.offset.PendingOffsetsAppender;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;

public class InMemoryReceiverFactory implements ReceiverFactory {
  private final MessageReceiver inMemoryMessageReader;

  public InMemoryReceiverFactory(InMemoryMessageReceiver inMemoryMessageReceiver) {
    this.inMemoryMessageReader = inMemoryMessageReceiver;
  }

  @Override
  public MessageReceiver createMessageReceiver(
      Topic receivingTopic,
      Subscription subscription,
      ConsumerRateLimiter consumerRateLimiter,
      SubscriptionLoadRecorder subscriptionLoadRecorder,
      MetricsFacade metrics,
      PendingOffsetsAppender pendingOffsetsAppender) {

    return inMemoryMessageReader;
  }
}
