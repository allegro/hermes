package pl.allegro.tech.hermes.consumers.consumer.filtering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.chain.FilterResult;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.function.Consumer;

import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

public class FilteredMessageHandler {

    private final ConsumerRateLimiter consumerRateLimiter;
    private final Trackers trackers;
    private final HermesMetrics metrics;

    private static final Logger logger = LoggerFactory.getLogger(FilteredMessageHandler.class);

    public FilteredMessageHandler(ConsumerRateLimiter consumerRateLimiter,
                                  Trackers trackers,
                                  HermesMetrics metrics) {
        this.consumerRateLimiter = consumerRateLimiter;
        this.trackers = trackers;
        this.metrics = metrics;
    }

    public void handle(FilterResult result, Message message, Subscription subscription, Consumer<SubscriptionPartitionOffset> committer) {
        if (result.isFiltered()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Message filtered for subscription {} {}", subscription.getQualifiedName(), result);
            }

            committer.accept(SubscriptionPartitionOffset.subscriptionPartitionOffset(message, subscription));

            updateMetrics(message, subscription);

            if (subscription.isTrackingEnabled()) {
                trackers.get(subscription).logFiltered(toMessageMetadata(message, subscription), result.getFilterType().get());
            }

            consumerRateLimiter.acquire();
        }
    }

    protected void updateMetrics(Message message, Subscription subscription) {
        metrics.meter(Meters.FILTERED_METER).mark();
        metrics.counter(Counters.FILTERED, subscription.getTopicName(), subscription.getName()).inc();
    }
}