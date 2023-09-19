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

import static pl.allegro.tech.hermes.common.metric.SubscriptionTagsFactory.subscriptionTags;

public class SubscriptionMetrics {
    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;

    public SubscriptionMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
    }

    public SubscriptionHermesCounter throughputInBytes(SubscriptionName subscription) {
        return SubscriptionHermesCounter.from(
                micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_THROUGHPUT, subscription),
                hermesMetrics.meter(Meters.SUBSCRIPTION_THROUGHPUT_BYTES, subscription.getTopicName(), subscription.getName()),
                Meters.SUBSCRIPTION_THROUGHPUT_BYTES, subscription);
    }

    public HermesCounter successes(SubscriptionName subscription) {
        return size -> {
            hermesMetrics.meter(Meters.METER).mark(size);
            hermesMetrics.meter(Meters.TOPIC_METER, subscription.getTopicName()).mark(size);
            hermesMetrics.meter(Meters.SUBSCRIPTION_METER, subscription.getTopicName(), subscription.getName()).mark(size);
            hermesMetrics.counter(Counters.DELIVERED, subscription.getTopicName(), subscription.getName()).inc(size);
            micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_DELIVERED, subscription).increment(size);
        };
    }

    public HermesCounter batchSuccesses(SubscriptionName subscription) {
        return HermesCounters.from(
                micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_BATCHES, subscription),
                hermesMetrics.meter(Meters.SUBSCRIPTION_BATCH_METER, subscription.getTopicName(), subscription.getName())
        );
    }

    public HermesCounter discarded(SubscriptionName subscription) {
        return size -> {
            hermesMetrics.meter(Meters.DISCARDED_METER).mark(size);
            hermesMetrics.meter(Meters.DISCARDED_TOPIC_METER, subscription.getTopicName()).mark(size);
            hermesMetrics.meter(Meters.DISCARDED_SUBSCRIPTION_METER, subscription.getTopicName(), subscription.getName()).mark(size);
            hermesMetrics.counter(Counters.DISCARDED, subscription.getTopicName(), subscription.getName()).inc(size);
            micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_DISCARDED, subscription).increment(size);
        };
    }

    public HermesTimer latency(SubscriptionName subscription) {
        return HermesTimer.from(
                meterRegistry.timer(SubscriptionMetricsNames.SUBSCRIPTION_LATENCY, subscriptionTags(subscription)),
                hermesMetrics.timer(Timers.SUBSCRIPTION_LATENCY, subscription.getTopicName(), subscription.getName())
        );
    }

    public <T> void registerInflightGauge(SubscriptionName subscription, T obj, ToDoubleFunction<T> f) {
        hermesMetrics.registerInflightGauge(subscription, () -> (int) f.applyAsDouble(obj));
        meterRegistry.gauge(SubscriptionMetricsNames.SUBSCRIPTION_INFLIGHT, subscriptionTags(subscription), obj, f);
    }

    public HermesTimer consumerIdleTimer(SubscriptionName subscription) {
        return HermesTimer.from(
                meterRegistry.timer(SubscriptionMetricsNames.SUBSCRIPTION_IDLE_DURATION, subscriptionTags(subscription)),
                hermesMetrics.timer(Timers.CONSUMER_IDLE_TIME, subscription.getTopicName(), subscription.getName())
        );
    }

    public HermesCounter filteredOutCounter(SubscriptionName subscription) {
        return HermesCounters.from(
                micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_FILTERED_OUT, subscription),
                hermesMetrics.meter(Meters.FILTERED_METER, subscription.getTopicName(), subscription.getName())
        );
    }

    public HermesCounter httpAnswerCounter(SubscriptionName subscription, int statusCode) {
        return size -> {
            meterRegistry.counter(
                    SubscriptionMetricsNames.SUBSCRIPTION_HTTP_STATUS_CODES,
                    Tags.concat(subscriptionTags(subscription), "status_code", String.valueOf(statusCode))
            ).increment(size);
            hermesMetrics.registerConsumerHttpAnswer(subscription, statusCode, size);
        };
    }

    public HermesCounter timeoutsCounter(SubscriptionName subscription) {
        return HermesCounters.from(
                micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_TIMEOUTS, subscription),
                hermesMetrics.consumerErrorsTimeoutMeter(subscription)
        );
    }

    public HermesCounter otherErrorsCounter(SubscriptionName subscription) {
        return HermesCounters.from(
                micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_OTHER_ERRORS, subscription),
                hermesMetrics.consumerErrorsOtherMeter(subscription)
        );
    }

    public HermesCounter failuresCounter(SubscriptionName subscription) {
        return HermesCounters.from(
                micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_FAILURES, subscription),
                hermesMetrics.meter(Meters.FAILED_METER_SUBSCRIPTION, subscription.getTopicName(), subscription.getName())
        );
    }

    public HermesHistogram inflightTimeInMillisHistogram(SubscriptionName subscriptionName) {
        return value -> {
            DistributionSummary.builder(SubscriptionMetricsNames.SUBSCRIPTION_INFLIGHT_TIME)
                    .tags(subscriptionTags(subscriptionName))
                    .register(meterRegistry)
                    .record(value / 1000d);
            hermesMetrics.inflightTimeHistogram(subscriptionName).update(value);
        };
    }

    private Counter micrometerCounter(String metricName, SubscriptionName subscription) {
        return meterRegistry.counter(metricName, subscriptionTags(subscription));
    }

    public static class SubscriptionMetricsNames {
        public static final String SUBSCRIPTION_DELIVERED = "subscription.delivered";
        public static final String SUBSCRIPTION_THROUGHPUT = "subscription.throughput-bytes";
        public static final String SUBSCRIPTION_BATCHES = "subscription.batches";
        public static final String SUBSCRIPTION_DISCARDED = "subscription.discarded";
        public static final String SUBSCRIPTION_LATENCY = "subscription.latency";
        public static final String SUBSCRIPTION_INFLIGHT = "subscription.inflight";
        public static final String SUBSCRIPTION_IDLE_DURATION = "subscription.idle-duration";
        public static final String SUBSCRIPTION_FILTERED_OUT = "subscription.filtered-out";
        public static final String SUBSCRIPTION_HTTP_STATUS_CODES = "subscription.http-status-codes";
        public static final String SUBSCRIPTION_TIMEOUTS = "subscription.timeouts";
        public static final String SUBSCRIPTION_OTHER_ERRORS = "subscription.other-errors";
        public static final String SUBSCRIPTION_FAILURES = "subscription.failures";
        public static final String SUBSCRIPTION_INFLIGHT_TIME = "subscription.inflight-time-seconds";
    }

}
