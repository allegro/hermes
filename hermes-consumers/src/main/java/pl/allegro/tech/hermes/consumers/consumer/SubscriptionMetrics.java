package pl.allegro.tech.hermes.consumers.consumer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;

import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;
import static pl.allegro.tech.hermes.common.metric.Counters.DELIVERED;
import static pl.allegro.tech.hermes.common.metric.Counters.DISCARDED;
import static pl.allegro.tech.hermes.common.metric.Counters.MAXRATE_FETCH_FAILURES;
import static pl.allegro.tech.hermes.common.metric.Counters.MAXRATE_RATE_HISTORY_FAILURES;
import static pl.allegro.tech.hermes.common.metric.Gauges.MAX_RATE_ACTUAL_RATE_VALUE;
import static pl.allegro.tech.hermes.common.metric.Gauges.MAX_RATE_VALUE;
import static pl.allegro.tech.hermes.common.metric.Gauges.OUTPUT_RATE;
import static pl.allegro.tech.hermes.common.metric.Meters.DISCARDED_METER;
import static pl.allegro.tech.hermes.common.metric.Meters.DISCARDED_SUBSCRIPTION_METER;
import static pl.allegro.tech.hermes.common.metric.Meters.DISCARDED_TOPIC_METER;
import static pl.allegro.tech.hermes.common.metric.Meters.FAILED_METER_SUBSCRIPTION;
import static pl.allegro.tech.hermes.common.metric.Meters.FILTERED_METER;
import static pl.allegro.tech.hermes.common.metric.Meters.METER;
import static pl.allegro.tech.hermes.common.metric.Meters.SUBSCRIPTION_BATCH_METER;
import static pl.allegro.tech.hermes.common.metric.Meters.SUBSCRIPTION_METER;
import static pl.allegro.tech.hermes.common.metric.Meters.SUBSCRIPTION_THROUGHPUT_BYTES;
import static pl.allegro.tech.hermes.common.metric.Meters.TOPIC_METER;
import static pl.allegro.tech.hermes.common.metric.Timers.CONSUMER_IDLE_TIME;
import static pl.allegro.tech.hermes.common.metric.Timers.SUBSCRIPTION_LATENCY;

public class SubscriptionMetrics {

    private final HermesMetrics metrics;
    private final SubscriptionName subscription;

    public SubscriptionMetrics(HermesMetrics metrics, SubscriptionName subscription) {
        this.metrics = metrics;
        this.subscription = subscription;
    }

    public void markAttempt() {
        metrics.incrementInflightCounter(subscription);
    }

    public void markSuccess(MessageBatch batch, MessageSendingResult result) {
        metrics.meter(METER).mark(batch.getMessageCount());
        metrics.meter(TOPIC_METER, subscription.getTopicName()).mark(batch.getMessageCount());
        metrics.meter(SUBSCRIPTION_METER, subscription.getTopicName(), subscription.getName()).mark(batch.getMessageCount());
        metrics.meter(SUBSCRIPTION_BATCH_METER, subscription.getTopicName(), subscription.getName()).mark();
        metrics.meter(SUBSCRIPTION_THROUGHPUT_BYTES, subscription.getTopicName(), subscription.getName()).mark(batch.getSize());
        metrics.registerConsumerHttpAnswer(subscription, result.getStatusCode());
        metrics.counter(DELIVERED, subscription.getTopicName(), subscription.getName()).inc(batch.getMessageCount());
        metrics.decrementInflightCounter(subscription, batch.getMessageCount());
        metrics.inflightTimeHistogram(subscription).update(batch.getLifetime());
    }

    public void markSuccess(Message message, MessageSendingResult result) {
        metrics.meter(METER).mark();
        metrics.meter(TOPIC_METER, subscription.getTopicName()).mark();
        metrics.meter(SUBSCRIPTION_METER, subscription.getTopicName(), subscription.getName()).mark();
        metrics.meter(SUBSCRIPTION_THROUGHPUT_BYTES, subscription.getTopicName(), subscription.getName()).mark(message.getSize());
        metrics.registerConsumerHttpAnswer(subscription, result.getStatusCode());
        metrics.counter(DELIVERED, subscription.getTopicName(), subscription.getName()).inc();
        metrics.decrementInflightCounter(subscription);
        metrics.inflightTimeHistogram(subscription).update(System.currentTimeMillis() - message.getReadingTimestamp());
    }

    public void markFailure(MessageBatch batch, MessageSendingResult result) {
        registerFailureMetrics(result, batch.getSize());
    }

    public void markFailure(Message message, MessageSendingResult result) {
        registerFailureMetrics(result, message.getSize());
    }

    private void registerFailureMetrics(MessageSendingResult result, long messageSize) {
        metrics.meter(FAILED_METER_SUBSCRIPTION, subscription.getTopicName(), subscription.getName()).mark();
        if (result.hasHttpAnswer()) {
            metrics.registerConsumerHttpAnswer(subscription, result.getStatusCode());
        } else if (result.isTimeout()) {
            metrics.consumerErrorsTimeoutMeter(subscription).mark();
        } else {
            metrics.consumerErrorsOtherMeter(subscription).mark();
        }
        metrics.meter(SUBSCRIPTION_THROUGHPUT_BYTES, subscription.getTopicName(), subscription.getName()).mark(messageSize);
    }

    public void markDiscarded(Message message) {
        metrics.meter(DISCARDED_METER).mark();
        metrics.meter(DISCARDED_TOPIC_METER, subscription.getTopicName()).mark();
        metrics.meter(DISCARDED_SUBSCRIPTION_METER, subscription.getTopicName(), subscription.getName()).mark();
        metrics.counter(DISCARDED, subscription.getTopicName(), subscription.getName()).inc();
        metrics.decrementInflightCounter(subscription);
        metrics.inflightTimeHistogram(subscription).update(System.currentTimeMillis() - message.getReadingTimestamp());
    }

    public void markDiscarded(MessageBatch batch) {
        metrics.meter(DISCARDED_METER).mark(batch.getMessageCount());
        metrics.meter(DISCARDED_TOPIC_METER, subscription.getTopicName()).mark(batch.getMessageCount());
        metrics.meter(DISCARDED_SUBSCRIPTION_METER, subscription.getTopicName(), subscription.getName()).mark(batch.getMessageCount());
        metrics.counter(DISCARDED, subscription.getTopicName(), subscription.getName()).inc(batch.getMessageCount());
        metrics.decrementInflightCounter(subscription, batch.getMessageCount());
        metrics.inflightTimeHistogram(subscription).update(batch.getLifetime());
    }

    public void markDiscarded(MessageMetadata messageMetadata) {
        TopicName topicName = fromQualifiedName(messageMetadata.getTopic());
        metrics.counter(DISCARDED, topicName, messageMetadata.getSubscription()).inc();
        metrics.meter(DISCARDED_METER).mark();
        metrics.meter(DISCARDED_TOPIC_METER, topicName).mark();
        metrics.meter(DISCARDED_SUBSCRIPTION_METER, topicName, messageMetadata.getSubscription()).mark();
        metrics.decrementInflightCounter(subscription);
    }

    public Timer subscriptionLatencyTimer() {
        return metrics.timer(SUBSCRIPTION_LATENCY, subscription.getTopicName(), subscription.getName());
    }

    public void markFilteredOut() {
        metrics.meter(FILTERED_METER, subscription.getTopicName(), subscription.getName()).mark();
    }

    public Timer consumerIdleTimer() {
        return metrics.timer(CONSUMER_IDLE_TIME, subscription.getTopicName(), subscription.getName());
    }

    public Counter rateHistoryFailuresCounter() {
        return metrics.counter(MAXRATE_RATE_HISTORY_FAILURES, subscription.getTopicName(), subscription.getName());
    }

    public Counter maxRateFetchFailuresCounter() {
        return metrics.counter(MAXRATE_FETCH_FAILURES, subscription.getTopicName(), subscription.getName());
    }

    public void registerMaxRateGauge(Gauge<Double> gauge) {
        metrics.registerGauge(MAX_RATE_VALUE, subscription, gauge);
    }

    public void registerRateGauge(Gauge<Double> gauge) {
        metrics.registerGauge(MAX_RATE_ACTUAL_RATE_VALUE, subscription, gauge);
    }

    public void registerOutputRateGauge(Gauge<Double> gauge) {
        metrics.registerGauge(OUTPUT_RATE, subscription, gauge);
    }

    public void shutdown() {
        metrics.unregister(DISCARDED_SUBSCRIPTION_METER, subscription);
        metrics.unregister(FAILED_METER_SUBSCRIPTION, subscription);
        metrics.unregister(SUBSCRIPTION_BATCH_METER, subscription);
        metrics.unregister(SUBSCRIPTION_METER, subscription);
        metrics.unregister(DELIVERED, subscription);
        metrics.unregister(DISCARDED, subscription);
        metrics.unregisterInflightCounter(subscription);
        metrics.unregisterInflightTimeHistogram(subscription);
        metrics.unregisterConsumerErrorsTimeoutMeter(subscription);
        metrics.unregisterConsumerErrorsOtherMeter(subscription);
        metrics.unregisterStatusMeters(subscription);
        metrics.unregister(OUTPUT_RATE, subscription);
        metrics.unregister(MAX_RATE_ACTUAL_RATE_VALUE, subscription);
        metrics.unregister(MAX_RATE_VALUE, subscription);
        metrics.unregister(MAXRATE_FETCH_FAILURES, subscription);
        metrics.unregister(MAXRATE_RATE_HISTORY_FAILURES, subscription);
        metrics.unregister(CONSUMER_IDLE_TIME, subscription);
        metrics.unregister(FILTERED_METER, subscription);
        metrics.unregister(SUBSCRIPTION_LATENCY, subscription);
        metrics.unregister(SUBSCRIPTION_THROUGHPUT_BYTES, subscription);
    }
}
