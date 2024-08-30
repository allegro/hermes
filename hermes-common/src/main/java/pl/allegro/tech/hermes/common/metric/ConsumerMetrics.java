package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.common.metric.SubscriptionTagsFactory.subscriptionTags;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.function.ToDoubleFunction;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.counters.HermesCounters;

public class ConsumerMetrics {
  private final MeterRegistry meterRegistry;
  private final GaugeRegistrar gaugeRegistrar;

  public ConsumerMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.gaugeRegistrar = new GaugeRegistrar(meterRegistry);
  }

  public <T> void registerQueueUtilizationGauge(T obj, String queueName, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge("queue." + queueName + ".utilization", obj, f);
  }

  public HermesCounter queueFailuresCounter(String name) {
    return HermesCounters.from(meterRegistry.counter("queue." + name + ".failures"));
  }

  public <T> void registerConsumerProcessesThreadsGauge(T obj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge("consumer-processes.threads", obj, f);
  }

  public <T> void registerRunningConsumerProcessesGauge(T obj, ToDoubleFunction<T> f) {
    meterRegistry.gauge("consumer-processes.running", obj, f);
  }

  public <T> void registerDyingConsumerProcessesGauge(T obj, ToDoubleFunction<T> f) {
    meterRegistry.gauge("consumer-processes.dying", obj, f);
  }

  public <T> void registerBatchBufferTotalBytesGauge(T obj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge("batch-buffer.total-bytes", obj, f);
  }

  public <T> void registerBatchBufferAvailableBytesGauge(T obj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge("batch-buffer.available-bytes", obj, f);
  }

  public HermesCounter oAuthSubscriptionTokenRequestCounter(
      Subscription subscription, String providerName) {
    return HermesCounters.from(
        meterRegistry.counter(
            "oauth.token-requests",
            Tags.concat(
                subscriptionTags(subscription.getQualifiedName()), "provider", providerName)));
  }

  public HermesTimer oAuthProviderLatencyTimer(String providerName) {
    return HermesTimer.from(
        meterRegistry.timer("oauth.token-request-latency", Tags.of("provider", providerName)));
  }

  public HermesCounter processedSignalsCounter(String name) {
    return HermesCounters.from(meterRegistry.counter("signals.processed", Tags.of("signal", name)));
  }

  public HermesCounter droppedSignalsCounter(String name) {
    return HermesCounters.from(meterRegistry.counter("signals.dropped", Tags.of("signal", name)));
  }
}
