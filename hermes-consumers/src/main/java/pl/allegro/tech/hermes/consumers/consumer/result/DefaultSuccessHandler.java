package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

public class DefaultSuccessHandler extends AbstractHandler implements SuccessHandler {

    private final Trackers trackers;

    public DefaultSuccessHandler(SubscriptionOffsetCommitQueues offsetHelper, HermesMetrics hermesMetrics, Trackers trackers) {
        super(offsetHelper, hermesMetrics);
        this.trackers = trackers;
    }

    @Override
    public void handle(Message message, Subscription subscription) {
        offsetHelper.decrement(message.getPartition(), message.getOffset());
        updateMeters(subscription);
        updateMetrics(Counters.CONSUMER_DELIVERED, message, subscription);
        trackers.get(subscription).logSent(toMessageMetadata(message, subscription));
    }

    private void updateMeters(Subscription subscription) {
        hermesMetrics.meter(Meters.CONSUMER_METER).mark();
        hermesMetrics.meter(Meters.CONSUMER_TOPIC_METER, subscription.getTopicName()).mark();
        hermesMetrics.meter(Meters.CONSUMER_SUBSCRIPTION_METER, subscription.getTopicName(), subscription.getName()).mark();
    }
}
