package pl.allegro.tech.hermes.common.metric.executor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class InstrumentedExecutorServiceFactory {

    private final MetricsFacade metricsFacade;
    private final RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

    public InstrumentedExecutorServiceFactory(MetricsFacade metricsFacade) {
        this.metricsFacade = metricsFacade;
    }

    public ExecutorService getExecutorService(String name, int size, boolean monitoringEnabled) {
        return getExecutorService(name, size, monitoringEnabled, Integer.MAX_VALUE);
    }

    public ExecutorService getExecutorService(String name, int size, boolean monitoringEnabled, int queueCapacity) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(name + "-executor-%d").build();
        ThreadPoolExecutor executor = newFixedThreadPool(name, size, threadFactory, queueCapacity);
        executor.prestartAllCoreThreads();

        return monitoringEnabled ? monitor(name, executor) : executor;
    }

    public ScheduledExecutorService getScheduledExecutorService(
            String name, int size, boolean monitoringEnabled, boolean removeOnCancel
    ) {

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(name + "-scheduled-executor-%d").build();
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(size, threadFactory);
        executor.setRemoveOnCancelPolicy(removeOnCancel);
        return monitoringEnabled ? monitor(name, executor) : executor;
    }

    private ExecutorService monitor(String threadPoolName, ExecutorService executor) {
        return metricsFacade.executor().monitor(executor, threadPoolName);
    }

    private ScheduledExecutorService monitor(String threadPoolName, ScheduledExecutorService executor) {
        return metricsFacade.executor().monitor(executor, threadPoolName);
    }

    /**
     * Copy of {@link java.util.concurrent.Executors#newFixedThreadPool(int, java.util.concurrent.ThreadFactory)}
     * with configurable queue capacity.
     */
    private ThreadPoolExecutor newFixedThreadPool(String executorName, int size, ThreadFactory threadFactory, int queueCapacity) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                size,
                size,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                threadFactory,
                rejectedExecutionHandler
        );
        return executor;
    }
}
