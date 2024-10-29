package pl.allegro.tech.hermes.consumers.consumer.filtering;

import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset.subscriptionPartitionOffset;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.PendingOffsetsAppender;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterResult;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

public class FilteredMessageHandler {

  private final PendingOffsetsAppender pendingOffsets;
  private final Optional<ConsumerRateLimiter> consumerRateLimiter;
  private final Trackers trackers;
  private final HermesCounter filteredOutCounter;

  private static final Logger logger = LoggerFactory.getLogger(FilteredMessageHandler.class);

  public FilteredMessageHandler(
      ConsumerRateLimiter consumerRateLimiter,
      PendingOffsetsAppender pendingOffsetsAppender,
      Trackers trackers,
      MetricsFacade metrics,
      SubscriptionName subscriptionName) {
    this.consumerRateLimiter = Optional.ofNullable(consumerRateLimiter);
    this.pendingOffsets = pendingOffsetsAppender;
    this.trackers = trackers;
    this.filteredOutCounter = metrics.subscriptions().filteredOutCounter(subscriptionName);
  }

  public void handle(FilterResult result, Message message, Subscription subscription) {
    if (result.isFiltered()) {
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Message filtered for subscription {} {}", subscription.getQualifiedName(), result);
      }

      pendingOffsets.markAsProcessed(
          subscriptionPartitionOffset(
              subscription.getQualifiedName(),
              message.getPartitionOffset(),
              message.getPartitionAssignmentTerm()));

      filteredOutCounter.increment();

      if (subscription.isTrackingEnabled()) {
        trackers
            .get(subscription)
            .logFiltered(toMessageMetadata(message, subscription), result.getFilterType().get());
      }

      consumerRateLimiter.ifPresent(ConsumerRateLimiter::acquireFiltered);
    }
  }
}
