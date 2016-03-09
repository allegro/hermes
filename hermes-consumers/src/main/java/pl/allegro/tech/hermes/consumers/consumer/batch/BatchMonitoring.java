package pl.allegro.tech.hermes.consumers.consumer.batch;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;
import static pl.allegro.tech.hermes.common.metric.Meters.METER;
import static pl.allegro.tech.hermes.common.metric.Meters.SUBSCRIPTION_METER;
import static pl.allegro.tech.hermes.common.metric.Meters.SUBSCRIPTION_BATCH_METER;
import static pl.allegro.tech.hermes.common.metric.Meters.TOPIC_METER;

public class BatchMonitoring {
    private HermesMetrics metrics;
    private Trackers trackers;

    public BatchMonitoring(HermesMetrics metrics, Trackers trackers) {
        this.metrics = metrics;
        this.trackers = trackers;
    }

    public void closeInflightMetrics(MessageBatch batch, Subscription subscription) {
        metrics.decrementInflightCounter(subscription, batch.size());
        metrics.inflightTimeHistogram(subscription).update(batch.getLifetime());
    }

    public void markSendingResult(MessageBatch batch, Subscription subscription, MessageSendingResult result) {
        metrics.registerConsumerHttpAnswer(subscription, result.getStatusCode());
        if (result.succeeded()) {
            markDelivered(batch, subscription);
        } else {
            markDiscarded(batch, subscription, "Retry policy exhausted with status code " + result.getStatusCode());
        }
    }

    private void markDelivered(MessageBatch batch, Subscription subscription) {
        metrics.meter(METER).mark(batch.size());
        metrics.meter(TOPIC_METER, subscription.getTopicName()).mark(batch.size());
        metrics.meter(SUBSCRIPTION_METER, subscription.getTopicName(), subscription.getName()).mark(batch.size());
        metrics.meter(SUBSCRIPTION_BATCH_METER, subscription.getTopicName(), subscription.getName()).mark();
        metrics.counter(Counters.DELIVERED, subscription.getTopicName(), subscription.getName()).inc(batch.size());
        batch.getMessagesMetadata().forEach(m -> trackers.get(subscription).logSent(m));
    }

    public void markDiscarded(MessageBatch batch, Subscription subscription, String reason) {
        metrics.counter(Counters.DISCARDED, subscription.getTopicName(), subscription.getName()).inc(batch.size());
        metrics.meter(Meters.DISCARDED_METER).mark(batch.size());
        metrics.meter(Meters.DISCARDED_TOPIC_METER, subscription.getTopicName()).mark(batch.size());
        metrics.meter(Meters.DISCARDED_SUBSCRIPTION_METER, subscription.getTopicName(), subscription.getName()).mark(batch.size());
        batch.getMessagesMetadata().forEach(m -> trackers.get(subscription).logDiscarded(m, reason));
    }

    public void markFailed(MessageBatch batch, Subscription subscription, MessageSendingResult result) {
        metrics.registerConsumerHttpAnswer(subscription, result.getStatusCode());
        metrics.meter(Meters.FAILED_METER_SUBSCRIPTION, subscription.getTopicName(), subscription.getName()).mark();
        registerFailureMetrics(subscription, result);
        batch.getMessagesMetadata().forEach(m -> trackers.get(subscription).logFailed(m, result.getRootCause()));
    }

    private void registerFailureMetrics(Subscription subscription, MessageSendingResult result) {
        if (result.hasHttpAnswer()) {
            metrics.registerConsumerHttpAnswer(subscription, result.getStatusCode());
        } else {
            (result.isTimeout() ? metrics.consumerErrorsTimeoutMeter(subscription) : metrics.consumerErrorsOtherMeter(subscription)).mark();
        }
    }

    public void markDiscarded(MessageMetadata messageMetadata, Subscription subscription, String reason) {
        TopicName topicName = fromQualifiedName(messageMetadata.getTopic());
        metrics.counter(Counters.DISCARDED, topicName, messageMetadata.getSubscription()).inc();
        metrics.meter(Meters.DISCARDED_METER).mark();
        metrics.meter(Meters.DISCARDED_TOPIC_METER, topicName).mark();
        metrics.meter(Meters.DISCARDED_SUBSCRIPTION_METER, topicName, messageMetadata.getSubscription()).mark();
        metrics.decrementInflightCounter(subscription);
        trackers.get(subscription).logDiscarded(messageMetadata, reason);
    }
}
