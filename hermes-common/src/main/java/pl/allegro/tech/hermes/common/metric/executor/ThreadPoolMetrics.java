package pl.allegro.tech.hermes.common.metric.executor;

import pl.allegro.tech.hermes.common.metric.MetricsFacade;

import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolMetrics {

    private final MetricsFacade metricsFacade;

    public ThreadPoolMetrics(MetricsFacade metricsFacade) {
        this.metricsFacade = metricsFacade;
    }

    public void createGauges(
            String executorName,
            ThreadPoolExecutor executor) {

        metricsFacade.executor().registerThreadPoolCapacity(executorName, executor, ThreadPoolExecutor::getPoolSize);
        metricsFacade.executor().registerThreadPoolActiveThreads(executorName, executor, ThreadPoolExecutor::getActiveCount);
        metricsFacade.executor().registerThreadPoolUtilization(executorName, executor,
                e -> (double) e.getActiveCount() / (double) e.getPoolSize()
        );
        metricsFacade.executor().registerThreadPoolTaskQueueCapacity(executorName, executor,
                e -> {
                    int qCapacity = e.getQueue().size() + e.getQueue().remainingCapacity();
                    // overflow in case of unbounded queue, set queueCapacity to Integer.MAX_VALUE
                    return qCapacity < 0 ? Integer.MAX_VALUE : qCapacity;
                });
        metricsFacade.executor().registerThreadPoolTaskQueued(executorName, executor, e -> e.getQueue().size());
        metricsFacade.executor().registerThreadPoolTaskQueueUtilization(executorName, executor,
                e -> {
                    int calculatedCapacity = e.getQueue().size() + e.getQueue().remainingCapacity();
                    int queueCapacity = calculatedCapacity < 0 ? Integer.MAX_VALUE : calculatedCapacity;
                    return (double) e.getQueue().size() / (double) queueCapacity;
                });
    }

    public void markRequestRejected(String executorName) {
        metricsFacade.executor().incrementRequestRejectedCounter(executorName);
    }

}
