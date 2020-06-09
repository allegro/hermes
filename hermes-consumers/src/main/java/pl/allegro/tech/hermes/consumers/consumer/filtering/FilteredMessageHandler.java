package pl.allegro.tech.hermes.consumers.consumer.filtering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.chain.FilterResult;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset.subscriptionPartitionOffset;

public class FilteredMessageHandler {

    private final OffsetQueue offsetQueue;
    private final ConsumerRateLimiter consumerRateLimiter;
    private final Trackers trackers;
    private final HermesMetrics metrics;

    private static final Logger logger = LoggerFactory.getLogger(FilteredMessageHandler.class);

    public FilteredMessageHandler(OffsetQueue offsetQueue,
                                  ConsumerRateLimiter consumerRateLimiter,
                                  Trackers trackers,
                                  HermesMetrics metrics) {
        this.offsetQueue = offsetQueue;
        this.consumerRateLimiter = consumerRateLimiter;
        this.trackers = trackers;
        this.metrics = metrics;
    }

    public void handle(FilterResult result, Message message, Subscription subscription) {
        if (result.isFiltered()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Message filtered for subscription {} {}", subscription.getQualifiedName(), result);
            }

            offsetQueue.offerCommittedOffset(subscriptionPartitionOffset(subscription.getQualifiedName(),
                    message.getPartitionOffset(), message.getPartitionAssignmentTerm()));

            updateMetrics(subscription);

            if(subscription.isTrackingEnabled()) {
                trackers.get(subscription).logFiltered(toMessageMetadata(message, subscription), result.getFilterType().get());
            }

            consumerRateLimiter.acquireFiltered();
        }
    }

    private void updateMetrics(Subscription subscription) {
        metrics.meter(Meters.FILTERED_METER, subscription.getTopicName(), subscription.getName()).mark();
    }
}
