package pl.allegro.tech.hermes.common.metric.executor;

import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolMetrics {

    private final HermesMetrics hermesMetrics;

    public ThreadPoolMetrics(HermesMetrics hermesMetrics) {
        this.hermesMetrics = hermesMetrics;
    }

    public void createGauges(
            String threadPoolName,
            ThreadPoolExecutor executor,
            BlockingQueue<Runnable> queue) {

        hermesMetrics.registerThreadPoolCapacity(threadPoolName, executor::getPoolSize);
        hermesMetrics.registerThreadPoolActiveThreads(threadPoolName, executor::getActiveCount);
        hermesMetrics.registerThreadPoolUtilization(threadPoolName,
                () -> (double) executor.getActiveCount() / (double) executor.getPoolSize()
        );
        hermesMetrics.registerThreadPoolTaskQueueCapacity(threadPoolName,
                () -> {
                    int qCapacity = queue.size() + queue.remainingCapacity();
                    // overflow in case of unbounded queue, set queueCapacity to Integer.MAX_VALUE
                    return qCapacity < 0 ? Integer.MAX_VALUE : qCapacity;
                });
        hermesMetrics.registerThreadPoolTaskQueued(threadPoolName, queue::size);
        hermesMetrics.registerThreadPoolTaskQueueUtilization(threadPoolName,
                () -> {
                    int calculatedCapacity = queue.size() + queue.remainingCapacity();
                    int queueCapacity = calculatedCapacity < 0 ? Integer.MAX_VALUE : calculatedCapacity;
                    return (double) queue.size() / (double) queueCapacity;
                });
    }

    public void markRequestRejected(String executorName) {
        hermesMetrics.incrementThreadPoolTaskRejectedCount(executorName);
    }

}
