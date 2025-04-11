package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.common.metric.SubscriptionTagsFactory.subscriptionTags;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.util.HashSet;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.subscription.metrics.MessageProcessingDurationMetricOptions;
import pl.allegro.tech.hermes.api.subscription.metrics.SubscriptionMetricConfig;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesHistogram;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.counters.HermesCounters;

public class SubscriptionMetrics {
  private final MeterRegistry meterRegistry;

  public SubscriptionMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public SubscriptionHermesCounter throughputInBytes(SubscriptionName subscription) {
    return SubscriptionHermesCounter.from(
        micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_THROUGHPUT, subscription),
        subscription);
  }

  public HermesCounter successes(SubscriptionName subscription) {
    return size ->
        micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_DELIVERED, subscription)
            .increment(size);
  }

  public HermesCounter batchSuccesses(SubscriptionName subscription) {
    return HermesCounters.from(
        micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_BATCHES, subscription));
  }

  public HermesCounter discarded(SubscriptionName subscription) {
    return size ->
        micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_DISCARDED, subscription)
            .increment(size);
  }

  public HermesCounter retries(SubscriptionName subscription) {
    return size ->
        micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_RETRIES, subscription)
            .increment(size);
  }

  public HermesTimer latency(SubscriptionName subscription) {
    return HermesTimer.from(
        meterRegistry.timer(
            SubscriptionMetricsNames.SUBSCRIPTION_LATENCY, subscriptionTags(subscription)));
  }

  public HermesTimer rateLimiterAcquire(SubscriptionName subscription) {
    return HermesTimer.from(
        meterRegistry.timer(
            SubscriptionMetricsNames.SUBSCRIPTION_RATE_LIMITER_ACQUIRE,
            subscriptionTags(subscription)));
  }

  public <T> void registerInflightGauge(
      SubscriptionName subscription, T obj, ToDoubleFunction<T> f) {
    meterRegistry.gauge(
        SubscriptionMetricsNames.SUBSCRIPTION_INFLIGHT, subscriptionTags(subscription), obj, f);
  }

  public <T> void registerPendingOffsetsGauge(
      SubscriptionName subscription, T obj, ToDoubleFunction<T> f) {
    meterRegistry.gauge(
        SubscriptionMetricsNames.SUBSCRIPTION_PENDING_OFFSETS,
        subscriptionTags(subscription),
        obj,
        f);
  }

  public HermesTimer consumerIdleTimer(SubscriptionName subscription) {
    return HermesTimer.from(
        meterRegistry.timer(
            SubscriptionMetricsNames.SUBSCRIPTION_IDLE_DURATION, subscriptionTags(subscription)));
  }

  public HermesCounter filteredOutCounter(SubscriptionName subscription) {
    return HermesCounters.from(
        micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_FILTERED_OUT, subscription));
  }

  public HermesCounter httpAnswerCounter(SubscriptionName subscription, int statusCode) {
    return size ->
        meterRegistry
            .counter(
                SubscriptionMetricsNames.SUBSCRIPTION_HTTP_STATUS_CODES,
                Tags.concat(
                    subscriptionTags(subscription), "status_code", String.valueOf(statusCode)))
            .increment(size);
  }

  public HermesCounter timeoutsCounter(SubscriptionName subscription) {
    return HermesCounters.from(
        micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_TIMEOUTS, subscription));
  }

  public HermesCounter otherErrorsCounter(SubscriptionName subscription) {
    return HermesCounters.from(
        micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_OTHER_ERRORS, subscription));
  }

  public HermesCounter failuresCounter(SubscriptionName subscription) {
    return HermesCounters.from(
        micrometerCounter(SubscriptionMetricsNames.SUBSCRIPTION_FAILURES, subscription));
  }

  public HermesHistogram inflightTimeInMillisHistogram(SubscriptionName subscriptionName) {
    return value ->
        DistributionSummary.builder(SubscriptionMetricsNames.SUBSCRIPTION_INFLIGHT_TIME)
            .tags(subscriptionTags(subscriptionName))
            .register(meterRegistry)
            .record(value / 1000d);
  }

  public HermesTimer messageProcessingTimeInMillisHistogram(
      SubscriptionName subscriptionName,
      SubscriptionMetricConfig<MessageProcessingDurationMetricOptions> metricConfig) {
    Set<Tag> subscriptionTags = subscriptionTags(subscriptionName);
    removeExistingMeter(SubscriptionMetricsNames.SUBSCRIPTION_PROCESSING_TIME, subscriptionTags);
    if (metricConfig.enabled() && metricConfig.options().hasThresholds()) {
      return HermesTimer.from(
          Timer.builder(SubscriptionMetricsNames.SUBSCRIPTION_PROCESSING_TIME)
              .tags(subscriptionTags)
              .serviceLevelObjectives(metricConfig.options().getThresholdsDurations())
              .register(meterRegistry));
    } else {
      return null;
    }
  }

  private Counter micrometerCounter(String metricName, SubscriptionName subscription) {
    return meterRegistry.counter(metricName, subscriptionTags(subscription));
  }

  private void removeExistingMeter(String metricName, Set<Tag> tags) {
    Stream<Meter> existingMeters =
        meterRegistry.find(metricName).meters().stream()
            .filter(it -> new HashSet<>(it.getId().getTags()).equals(tags));
    existingMeters.forEach(it -> meterRegistry.remove(it.getId()));
  }

  public static class SubscriptionMetricsNames {
    public static final String SUBSCRIPTION_DELIVERED = "subscription.delivered";
    public static final String SUBSCRIPTION_THROUGHPUT = "subscription.throughput-bytes";
    public static final String SUBSCRIPTION_BATCHES = "subscription.batches";
    public static final String SUBSCRIPTION_DISCARDED = "subscription.discarded";
    public static final String SUBSCRIPTION_RETRIES = "subscription.retries";
    public static final String SUBSCRIPTION_LATENCY = "subscription.latency";
    public static final String SUBSCRIPTION_RATE_LIMITER_ACQUIRE =
        "subscription.rate-limiter-acquire";
    public static final String SUBSCRIPTION_INFLIGHT = "subscription.inflight";
    public static final String SUBSCRIPTION_PENDING_OFFSETS = "subscription.pending-offsets";
    public static final String SUBSCRIPTION_IDLE_DURATION = "subscription.idle-duration";
    public static final String SUBSCRIPTION_FILTERED_OUT = "subscription.filtered-out";
    public static final String SUBSCRIPTION_HTTP_STATUS_CODES = "subscription.http-status-codes";
    public static final String SUBSCRIPTION_TIMEOUTS = "subscription.timeouts";
    public static final String SUBSCRIPTION_OTHER_ERRORS = "subscription.other-errors";
    public static final String SUBSCRIPTION_FAILURES = "subscription.failures";
    public static final String SUBSCRIPTION_INFLIGHT_TIME = "subscription.inflight-time-seconds";
    public static final String SUBSCRIPTION_PROCESSING_TIME =
        "subscription.message-processing-time";
  }
}
