package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.SubscriptionMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset.subscriptionPartitionOffset;

public class DefaultSuccessHandler implements SuccessHandler {

    private final Trackers trackers;
    private final OffsetQueue offsetQueue;
    private final SubscriptionMetrics metrics;

    public DefaultSuccessHandler(OffsetQueue offsetQueue, SubscriptionMetrics metrics, Trackers trackers) {
        this.offsetQueue = offsetQueue;
        this.metrics = metrics;
        this.trackers = trackers;
    }

    @Override
    public void handleSuccess(Message message, Subscription subscription, MessageSendingResult result) {
        offsetQueue.offerCommittedOffset(subscriptionPartitionOffset(subscription.getQualifiedName(),
                message.getPartitionOffset(), message.getPartitionAssignmentTerm()));
        metrics.markSuccess(message, result);
        trackers.get(subscription).logSent(toMessageMetadata(message, subscription), result.getHostname());
    }
}
