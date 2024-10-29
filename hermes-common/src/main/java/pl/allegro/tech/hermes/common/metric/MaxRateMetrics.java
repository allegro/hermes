package pl.allegro.tech.hermes.common.metric;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static pl.allegro.tech.hermes.common.metric.SubscriptionTagsFactory.subscriptionTags;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.function.ToDoubleFunction;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.counters.HermesCounters;

public class MaxRateMetrics {

  private final MeterRegistry meterRegistry;

  MaxRateMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public <T> void registerCalculationDurationInMillisGauge(T obj, ToDoubleFunction<T> f) {
    meterRegistry
        .more()
        .timeGauge("max-rate.calculation.duration", List.of(), obj, MILLISECONDS, f);
  }

  public HermesCounter historyUpdateFailuresCounter(SubscriptionName subscription) {
    return HermesCounters.from(
        meterRegistry.counter("max-rate.history-update.failures", subscriptionTags(subscription)));
  }

  public HermesCounter fetchFailuresCounter(SubscriptionName subscription) {
    return HermesCounters.from(
        meterRegistry.counter("max-rate.fetch.failures", subscriptionTags(subscription)));
  }

  public <T> void registerCalculatedRateGauge(
      SubscriptionName subscription, T obj, ToDoubleFunction<T> f) {
    meterRegistry.gauge("max-rate.calculated-rate", subscriptionTags(subscription), obj, f);
  }

  public <T> void registerActualRateGauge(
      SubscriptionName subscription, T obj, ToDoubleFunction<T> f) {
    meterRegistry.gauge("max-rate.actual-rate", subscriptionTags(subscription), obj, f);
  }

  public <T> void registerOutputRateGauge(
      SubscriptionName subscription, T obj, ToDoubleFunction<T> f) {
    meterRegistry.gauge("max-rate.output-rate", subscriptionTags(subscription), obj, f);
  }
}
