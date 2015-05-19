package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;
import pl.allegro.tech.hermes.consumers.message.tracker.Trackers;

import static pl.allegro.tech.hermes.common.metric.Metrics.Counter.CONSUMER_DELIVERED;
import static pl.allegro.tech.hermes.common.metric.Metrics.Meter.CONSUMER_METER;

public class DefaultSuccessHandler extends AbstractHandler implements SuccessHandler {

    private final Trackers trackers;

    public DefaultSuccessHandler(SubscriptionOffsetCommitQueues offsetHelper, HermesMetrics hermesMetrics, Trackers trackers) {
        super(offsetHelper, hermesMetrics);
        this.trackers = trackers;
    }

    @Override
    public void handle(Message message, Subscription subscription) {
        offsetHelper.decrement(message.getPartition(), message.getOffset());
        updateMetrics(CONSUMER_DELIVERED, CONSUMER_METER, message, subscription);
        trackers.get(subscription).logSent(message, subscription);
    }
}
