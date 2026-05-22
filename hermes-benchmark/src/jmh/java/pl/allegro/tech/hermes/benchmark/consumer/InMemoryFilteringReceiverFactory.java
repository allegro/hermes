package pl.allegro.tech.hermes.benchmark.consumer;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilteredMessageHandler;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.consumer.offset.PendingOffsetsAppender;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.FilteringMessageReceiver;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

public class InMemoryFilteringReceiverFactory implements ReceiverFactory {
  private final MessageReceiver inMemoryMessageReader;
  private final FilterChainFactory filterChainFactory;

  public InMemoryFilteringReceiverFactory(
      InMemoryMessageReceiver inMemoryMessageReceiver, FilterChainFactory filterChainFactory) {
    this.inMemoryMessageReader = inMemoryMessageReceiver;
    this.filterChainFactory = filterChainFactory;
  }

  @Override
  public MessageReceiver createMessageReceiver(
      Topic receivingTopic,
      Subscription subscription,
      ConsumerRateLimiter consumerRateLimiter,
      SubscriptionLoadRecorder subscriptionLoadRecorder,
      MetricsFacade metrics,
      PendingOffsetsAppender pendingOffsetsAppender) {

    FilteredMessageHandler filteredMessageHandler =
        new FilteredMessageHandler(
            new NoOpConsumerRateLimiter(),
            pendingOffsetsAppender,
            new Trackers(List.of()),
            new MetricsFacade(new SimpleMeterRegistry()),
            subscription.getQualifiedName());

    return new FilteringMessageReceiver(
        inMemoryMessageReader, filteredMessageHandler, filterChainFactory, subscription);
  }
}
