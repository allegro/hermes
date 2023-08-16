package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import java.util.function.ToDoubleFunction;

public class ExecutorMetrics {
    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;

    public ExecutorMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
    }

    public <T> void registerThreadPoolCapacity(String executorName, T stateObj, ToDoubleFunction<T> f) {
        hermesMetrics.registerThreadPoolCapacity(executorName, () -> (int) f.applyAsDouble(stateObj));
        registerMicrometerGauge("executors.capacity", executorName, stateObj, f);
    }

    public <T> void registerThreadPoolActiveThreads(String executorName, T stateObj, ToDoubleFunction<T> f) {
        hermesMetrics.registerThreadPoolActiveThreads(executorName, () -> (int) f.applyAsDouble(stateObj));
        registerMicrometerGauge("executors.active-threads", executorName, stateObj, f);
    }

    public <T> void registerThreadPoolUtilization(String executorName, T stateObj, ToDoubleFunction<T> f) {
        hermesMetrics.registerThreadPoolUtilization(executorName, () -> f.applyAsDouble(stateObj));
        registerMicrometerGauge("executors.utilization", executorName, stateObj, f);
    }

    public <T> void registerThreadPoolTaskQueueCapacity(String executorName, T stateObj, ToDoubleFunction<T> f) {
        hermesMetrics.registerThreadPoolTaskQueueCapacity(executorName, () -> (int) f.applyAsDouble(stateObj));
        registerMicrometerGauge("executors.task-queue-capacity", executorName, stateObj, f);
    }

    public <T> void registerThreadPoolTaskQueued(String executorName, T stateObj, ToDoubleFunction<T> f) {
        hermesMetrics.registerThreadPoolTaskQueued(executorName, () -> (int) f.applyAsDouble(stateObj));
        registerMicrometerGauge("executors.task-queue-size", executorName, stateObj, f);
    }

    public <T> void registerThreadPoolTaskQueueUtilization(String executorName, T stateObj, ToDoubleFunction<T> f) {
        hermesMetrics.registerThreadPoolTaskQueueUtilization(executorName, () -> f.applyAsDouble(stateObj));
        registerMicrometerGauge("executors.task-queue-utilization", executorName, stateObj, f);
    }

    public void incrementRequestRejectedCounter(String executorName) {
        hermesMetrics.incrementThreadPoolTaskRejectedCount(executorName);
        meterRegistry.counter("executors.task-rejected", Tags.of("executor_name", executorName)).increment();
    }


    private <T> void registerMicrometerGauge(String name, String executorName, T stateObj, ToDoubleFunction<T> f) {
        meterRegistry.gauge(name, Tags.of("executor_name", executorName), stateObj, f);
    }
}
