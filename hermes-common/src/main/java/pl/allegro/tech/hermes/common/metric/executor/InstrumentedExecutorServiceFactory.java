package pl.allegro.tech.hermes.common.metric.executor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newScheduledThreadPool;

public class InstrumentedExecutorServiceFactory {

    private final ThreadPoolMetrics threadPoolMetrics;
    private final RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

    public InstrumentedExecutorServiceFactory(ThreadPoolMetrics threadPoolMetrics) {
        this.threadPoolMetrics = threadPoolMetrics;
    }

    public ExecutorService getExecutorService(String name, int size, boolean monitoringEnabled) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(name + "-executor-%d").build();
        ThreadPoolExecutor executor = newFixedThreadPool(name, size, threadFactory);
        executor.prestartAllCoreThreads();

        if (monitoringEnabled) {
            monitor(name, executor);
        }

        return executor;
    }

    public ScheduledExecutorService getScheduledExecutorService(
            String name, int size, boolean monitoringEnabled
    ) {

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(name + "-scheduled-executor-%d").build();

        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(size, threadFactory);

        if (monitoringEnabled) {
            monitor(name, executor);
        }

        return executor;
    }

    private void monitor(String threadPoolName, ThreadPoolExecutor executor) {
        BlockingQueue<Runnable> queue = executor.getQueue();
        threadPoolMetrics.createGauges(threadPoolName, executor, queue);
    }


    /**
     * Copy of {@link java.util.concurrent.Executors#newFixedThreadPool(int, java.util.concurrent.ThreadFactory)}.
     */
    private ThreadPoolExecutor newFixedThreadPool(String executorName, int size, ThreadFactory threadFactory) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                size,
                size,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory,
                getMeteredRejectedExecutionHandler(executorName)
        );

        return executor;

    }

    RejectedExecutionHandler getMeteredRejectedExecutionHandler(String executorName) {
        return (r, executor) -> {
            threadPoolMetrics.markRequestRejected(executorName);
            rejectedExecutionHandler.rejectedExecution(r, executor);
        };
    }

}
