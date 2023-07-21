package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.counters.HermesCounters;

import java.util.function.ToDoubleFunction;

import static pl.allegro.tech.hermes.common.metric.Gauges.BATCH_BUFFER_AVAILABLE_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.BATCH_BUFFER_TOTAL_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.THREADS;
import static pl.allegro.tech.hermes.common.metric.SubscriptionTagsFactory.subscriptionTags;

public class ConsumerMetrics {
    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;
    private final GaugeRegistrar gaugeRegistrar;

    public ConsumerMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
        this.gaugeRegistrar = new GaugeRegistrar(meterRegistry, hermesMetrics);
    }

    public  <T> void registerQueueUtilizationGauge(T obj, String queueName, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge("queue." + queueName + ".utilization", obj, f);
    }

    public HermesCounter queueFailuresCounter(String name) {
        return HermesCounters.from(
                meterRegistry.counter("queue." + name + ".failures"),
                hermesMetrics.counter("queue." + name + ".failures")
        );
    }

    public <T> void registerConsumerProcessesThreadsGauge(T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(THREADS, "consumer-processes.threads", obj, f);
    }

    public <T> void registerRunningConsumerProcessesGauge(T obj, ToDoubleFunction<T> f) {
        hermesMetrics.registerRunningConsumerProcessesCountGauge(() -> (int) f.applyAsDouble(obj));
        meterRegistry.gauge("consumer-processes.running", obj, f);
    }

    public <T> void registerDyingConsumerProcessesGauge(T obj, ToDoubleFunction<T> f) {
        hermesMetrics.registerDyingConsumerProcessesCountGauge(() -> (int) f.applyAsDouble(obj));
        meterRegistry.gauge("consumer-processes.dying", obj, f);
    }

    public <T> void registerBatchBufferTotalBytesGauge(T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(BATCH_BUFFER_TOTAL_BYTES, "batch-buffer.total-bytes", obj, f);
    }

    public  <T> void registerBatchBufferAvailableBytesGauge(T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(BATCH_BUFFER_AVAILABLE_BYTES, "batch-buffer.available-bytes", obj, f);
    }

    public HermesCounter oAuthSubscriptionTokenRequestCounter(Subscription subscription, String providerName) {
        return HermesCounters.from(
                meterRegistry.counter("oauth.token-requests", Tags.concat(
                        subscriptionTags(subscription.getQualifiedName()),
                        "provider", providerName
                )),
                hermesMetrics.oAuthSubscriptionTokenRequestMeter(subscription, providerName)
        );
    }

    public HermesTimer oAuthProviderLatencyTimer(String providerName) {
        return HermesTimer.from(
                meterRegistry.timer("oauth.token-request-latency", Tags.of("provider", providerName)),
                hermesMetrics.oAuthProviderLatencyTimer(providerName)
        );
    }

    public HermesCounter processedSignalsCounter(String name) {
        return HermesCounters.from(
                meterRegistry.counter("signals.processed", Tags.of("signal", name)),
                hermesMetrics.counter("supervisor.signal." + name)
        );
    }

    public HermesCounter droppedSignalsCounter(String name) {
        return HermesCounters.from(
                meterRegistry.counter("signals.dropped", Tags.of("signal", name)),
                hermesMetrics.counter("supervisor.signal.dropped." + name)
        );
    }
}
