package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset.subscriptionPartitionOffset;

public class DefaultSuccessHandler implements SuccessHandler {

    private final Trackers trackers;
    private final OffsetQueue offsetQueue;
    private final MetricsFacade metrics;

    public DefaultSuccessHandler(OffsetQueue offsetQueue, MetricsFacade metrics, Trackers trackers) {
        this.offsetQueue = offsetQueue;
        this.metrics = metrics;
        this.trackers = trackers;
    }

    @Override
    public void handleSuccess(Message message, Subscription subscription, MessageSendingResult result) {
        offsetQueue.offerCommittedOffset(subscriptionPartitionOffset(subscription.getQualifiedName(),
                message.getPartitionOffset(), message.getPartitionAssignmentTerm()));
        markSuccess(message, subscription.getQualifiedName(), result);
        trackers.get(subscription).logSent(toMessageMetadata(message, subscription), result.getHostname());
    }

    private void markSuccess(Message message, SubscriptionName subscription, MessageSendingResult result) {
        metrics.subscriptions().successes(subscription).increment();
        metrics.subscriptions().throughputInBytes(subscription).increment(message.getSize());
        metrics.subscriptions().httpAnswerCounter(subscription, result.getStatusCode()).increment();
        metrics.subscriptions().inflightTimeInMillisHistogram(subscription).record(System.currentTimeMillis() - message.getReadingTimestamp());
    }
}
