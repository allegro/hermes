package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetCommitter;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

public class DefaultSuccessHandler extends AbstractHandler implements SuccessHandler {

    private final Trackers trackers;

    public DefaultSuccessHandler(OffsetQueue offsetQueue, HermesMetrics hermesMetrics, Trackers trackers) {
        super(offsetQueue, hermesMetrics);
        this.trackers = trackers;
    }

    @Override
    public void handleSuccess(Message message, Subscription subscription, MessageSendingResult result) {
        offsetQueue.offerCommittedOffset(SubscriptionPartitionOffset.subscriptionPartitionOffset(message, subscription));

        updateMeters(subscription, result);
        updateMetrics(Counters.DELIVERED, message, subscription);

        trackers.get(subscription).logSent(toMessageMetadata(message, subscription), result.getHostname());
    }

    private void updateMeters(Subscription subscription, MessageSendingResult result) {
        hermesMetrics.meter(Meters.METER).mark();
        hermesMetrics.meter(Meters.TOPIC_METER, subscription.getTopicName()).mark();
        hermesMetrics.meter(Meters.SUBSCRIPTION_METER, subscription.getTopicName(), subscription.getName()).mark();
        hermesMetrics.registerConsumerHttpAnswer(subscription, result.getStatusCode());
    }
}
