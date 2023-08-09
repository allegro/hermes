package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.counters.HermesCounters;

import java.util.List;
import java.util.function.ToDoubleFunction;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static pl.allegro.tech.hermes.common.metric.Counters.MAXRATE_FETCH_FAILURES;
import static pl.allegro.tech.hermes.common.metric.Counters.MAXRATE_RATE_HISTORY_FAILURES;
import static pl.allegro.tech.hermes.common.metric.Gauges.MAX_RATE_ACTUAL_RATE_VALUE;
import static pl.allegro.tech.hermes.common.metric.Gauges.MAX_RATE_CALCULATION_DURATION;
import static pl.allegro.tech.hermes.common.metric.Gauges.MAX_RATE_VALUE;
import static pl.allegro.tech.hermes.common.metric.Gauges.OUTPUT_RATE;
import static pl.allegro.tech.hermes.common.metric.SubscriptionTagsFactory.subscriptionTags;

public class MaxRateMetrics {

    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;

    MaxRateMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
    }

    public <T> void registerCalculationDurationInMillisGauge(T obj, ToDoubleFunction<T> f) {
        hermesMetrics.registerGauge(MAX_RATE_CALCULATION_DURATION, () -> (int) f.applyAsDouble(obj));
        meterRegistry.more().timeGauge("max-rate.calculation.duration", List.of(), obj, MILLISECONDS, f);
    }

    public HermesCounter historyUpdateFailuresCounter(SubscriptionName subscription) {
        return HermesCounters.from(
                meterRegistry.counter("max-rate.history-update.failures", subscriptionTags(subscription)),
                hermesMetrics.counter(MAXRATE_RATE_HISTORY_FAILURES, subscription.getTopicName(), subscription.getName())
        );
    }

    public HermesCounter fetchFailuresCounter(SubscriptionName subscription) {
        return HermesCounters.from(
                meterRegistry.counter("max-rate.fetch.failures", subscriptionTags(subscription)),
                hermesMetrics.counter(MAXRATE_FETCH_FAILURES, subscription.getTopicName(), subscription.getName())
        );
    }

    public <T> void registerCalculatedRateGauge(SubscriptionName subscription, T obj, ToDoubleFunction<T> f) {
        hermesMetrics.registerGauge(MAX_RATE_VALUE, subscription, () -> (int) f.applyAsDouble(obj));
        meterRegistry.gauge("max-rate.calculated-rate", subscriptionTags(subscription), obj, f);
    }

    public <T> void registerActualRateGauge(SubscriptionName subscription, T obj, ToDoubleFunction<T> f) {
        hermesMetrics.registerGauge(MAX_RATE_ACTUAL_RATE_VALUE, subscription, () -> (int) f.applyAsDouble(obj));
        meterRegistry.gauge("max-rate.actual-rate", subscriptionTags(subscription), obj, f);
    }

    public <T> void registerOutputRateGauge(SubscriptionName subscription, T obj, ToDoubleFunction<T> f) {
        hermesMetrics.registerGauge(OUTPUT_RATE, subscription, () -> (int) f.applyAsDouble(obj));
        // This metric provides almost the same information as Gauge.MAX_RATE_ACTUAL_RATE_VALUE does,
        // so there is no need to migrate it to Micrometer.
    }
}
