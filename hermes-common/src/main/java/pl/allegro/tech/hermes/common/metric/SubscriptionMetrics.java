package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesHistogram;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.counters.HermesCounters;

import java.util.function.ToDoubleFunction;

import static pl.allegro.tech.hermes.common.metric.Counters.DELIVERED;
import static pl.allegro.tech.hermes.common.metric.Counters.DISCARDED;
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
import static pl.allegro.tech.hermes.common.metric.SubscriptionTagsFactory.subscriptionTags;
import static pl.allegro.tech.hermes.common.metric.Timers.CONSUMER_IDLE_TIME;
import static pl.allegro.tech.hermes.common.metric.Timers.SUBSCRIPTION_LATENCY;

public class SubscriptionMetrics {
    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;

    public SubscriptionMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
    }

    public SubscriptionHermesCounter throughputInBytes(SubscriptionName subscription) {
        return SubscriptionHermesCounter.from(
                micrometerCounter("subscription-throughput-bytes", subscription),
                hermesMetrics.meter(SUBSCRIPTION_THROUGHPUT_BYTES, subscription.getTopicName(), subscription.getName()),
                SUBSCRIPTION_THROUGHPUT_BYTES, subscription);
    }

    public HermesCounter successes(SubscriptionName subscription) {
        return size -> {
            hermesMetrics.meter(METER).mark(size);
            hermesMetrics.meter(TOPIC_METER, subscription.getTopicName()).mark(size);
            hermesMetrics.meter(SUBSCRIPTION_METER, subscription.getTopicName(), subscription.getName()).mark(size);
            hermesMetrics.counter(DELIVERED, subscription.getTopicName(), subscription.getName()).inc(size);
            micrometerCounter("subscription.delivered", subscription).increment(size);
        };
    }

    public HermesCounter batchSuccesses(SubscriptionName subscription) {
        return HermesCounters.from(
                micrometerCounter("subscription.batches", subscription),
                hermesMetrics.meter(SUBSCRIPTION_BATCH_METER, subscription.getTopicName(), subscription.getName())
        );
    }

    public HermesCounter discarded(SubscriptionName subscription) {
        return size -> {
            hermesMetrics.meter(DISCARDED_METER).mark(size);
            hermesMetrics.meter(DISCARDED_TOPIC_METER, subscription.getTopicName()).mark(size);
            hermesMetrics.meter(DISCARDED_SUBSCRIPTION_METER, subscription.getTopicName(), subscription.getName()).mark(size);
            hermesMetrics.counter(DISCARDED, subscription.getTopicName(), subscription.getName()).inc(size);
            micrometerCounter("subscription.discarded", subscription).increment(size);
        };
    }

    public HermesTimer latency(SubscriptionName subscription) {
        return HermesTimer.from(
                meterRegistry.timer("subscription.latency", subscriptionTags(subscription)),
                hermesMetrics.timer(SUBSCRIPTION_LATENCY, subscription.getTopicName(), subscription.getName())
        );
    }

    public <T> void registerInflightGauge(SubscriptionName subscription, T obj, ToDoubleFunction<T> f) {
        hermesMetrics.registerInflightGauge(subscription, () -> (int) f.applyAsDouble(obj));
        meterRegistry.gauge("subscription.inflight", subscriptionTags(subscription), obj, f);
    }

    public HermesTimer consumerIdleTimer(SubscriptionName subscription) {
        return HermesTimer.from(
                meterRegistry.timer("subscription.idle-duration", subscriptionTags(subscription)),
                hermesMetrics.timer(CONSUMER_IDLE_TIME, subscription.getTopicName(), subscription.getName())
        );
    }

    public HermesCounter filteredOutCounter(SubscriptionName subscription) {
        return HermesCounters.from(
                micrometerCounter("subscription.filtered-out", subscription),
                hermesMetrics.meter(FILTERED_METER, subscription.getTopicName(), subscription.getName())
        );
    }

    public HermesCounter httpAnswerCounter(SubscriptionName subscription, int statusCode) {
        return size -> {
            meterRegistry.counter(
                    "subscription.http-status-codes",
                    Tags.concat(subscriptionTags(subscription), "status_code", String.valueOf(statusCode))
            ).increment(size);
            hermesMetrics.registerConsumerHttpAnswer(subscription, statusCode, size);
        };
    }

    public HermesCounter timeoutsCounter(SubscriptionName subscription) {
        return HermesCounters.from(
                micrometerCounter("subscription.timeouts", subscription),
                hermesMetrics.consumerErrorsTimeoutMeter(subscription)
        );
    }

    public HermesCounter otherErrorsCounter(SubscriptionName subscription) {
        return HermesCounters.from(
                micrometerCounter("subscription.other-errors", subscription),
                hermesMetrics.consumerErrorsOtherMeter(subscription)
        );
    }

    public HermesCounter failuresCounter(SubscriptionName subscription) {
        return HermesCounters.from(
                micrometerCounter("subscription.failures", subscription),
                hermesMetrics.meter(FAILED_METER_SUBSCRIPTION, subscription.getTopicName(), subscription.getName())
        );
    }

    public HermesHistogram inflightTimeInMillisHistogram(SubscriptionName subscriptionName) {
        return HermesHistogram.of(
                DistributionSummary.builder("subscription.inflight-time")
                        .baseUnit("ms")
                        .tags(subscriptionTags(subscriptionName))
                        .register(meterRegistry),
                hermesMetrics.inflightTimeHistogram(subscriptionName)
        );
    }

    private Counter micrometerCounter(String metricName, SubscriptionName subscription) {
        return meterRegistry.counter(metricName, subscriptionTags(subscription));
    }
}
