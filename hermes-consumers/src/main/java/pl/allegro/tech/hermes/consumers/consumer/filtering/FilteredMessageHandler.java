package pl.allegro.tech.hermes.consumers.consumer.filtering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.chain.FilterResult;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

public class FilteredMessageHandler {
    private final SubscriptionOffsetCommitQueues offsets;
    private final Trackers trackers;
    private final HermesMetrics metrics;

    private static final Logger logger = LoggerFactory.getLogger(FilteredMessageHandler.class);

    public FilteredMessageHandler(SubscriptionOffsetCommitQueues offsets, Trackers trackers, HermesMetrics metrics) {
        this.offsets = offsets;
        this.trackers = trackers;
        this.metrics = metrics;
    }

    public void handle(final FilterResult result, final Message message, final Subscription subscription) {
        if (result.isFiltered()) {
            logger.debug("Message filtered for subscription {} {}", subscription.getId(), result);
            offsets.remove(message);
            updateMetrics(message, subscription);
            trackers.get(subscription).logFiltered(toMessageMetadata(message, subscription), result.getFilterType().get());
        }
    }

    protected void updateMetrics(Message message, Subscription subscription) {
        metrics.meter(Meters.FILTERED_METER).mark();
        metrics.counter(Counters.FILTERED, subscription.getTopicName(), subscription.getName()).inc();
        metrics.decrementInflightCounter(subscription);
        metrics.inflightTimeHistogram(subscription).update(System.currentTimeMillis() - message.getReadingTimestamp());
    }
}
