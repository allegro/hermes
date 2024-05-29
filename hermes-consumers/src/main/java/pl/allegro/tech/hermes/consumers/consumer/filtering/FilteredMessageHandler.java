package pl.allegro.tech.hermes.consumers.consumer.filtering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsSlots;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterResult;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.Optional;

import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset.subscriptionPartitionOffset;

public class FilteredMessageHandler {

    private final OffsetsSlots offsetsSlots;
    private final Optional<ConsumerRateLimiter> consumerRateLimiter;
    private final Trackers trackers;
    private final HermesCounter filteredOutCounter;

    private static final Logger logger = LoggerFactory.getLogger(FilteredMessageHandler.class);

    public FilteredMessageHandler(ConsumerRateLimiter consumerRateLimiter,
                                  OffsetsSlots offsetsSlots,
                                  Trackers trackers,
                                  MetricsFacade metrics,
                                  SubscriptionName subscriptionName) {
        this.consumerRateLimiter = Optional.ofNullable(consumerRateLimiter);
        this.offsetsSlots = offsetsSlots;
        this.trackers = trackers;
        this.filteredOutCounter = metrics.subscriptions().filteredOutCounter(subscriptionName);
    }

    public void handle(FilterResult result, Message message, Subscription subscription) {
        if (result.isFiltered()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Message filtered for subscription {} {}", subscription.getQualifiedName(), result);
            }

            offsetsSlots.markAsSent(subscriptionPartitionOffset(subscription.getQualifiedName(),
                    message.getPartitionOffset(), message.getPartitionAssignmentTerm()));

            filteredOutCounter.increment();

            if (subscription.isTrackingEnabled()) {
                trackers.get(subscription).logFiltered(toMessageMetadata(message, subscription), result.getFilterType().get());
            }

            consumerRateLimiter.ifPresent(ConsumerRateLimiter::acquireFiltered);
        }
    }
}
