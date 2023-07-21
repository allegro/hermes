package pl.allegro.tech.hermes.consumers.queue;

import org.jctools.queues.MessagePassingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;

public class MonitoredMpscQueue<T> implements MpscQueue<T> {

    private static final Logger logger = LoggerFactory.getLogger(MonitoredMpscQueue.class);

    private final MpscQueue<T> queue;

    private final String name;

    private final MetricsFacade metrics;

    public MonitoredMpscQueue(MpscQueue<T> queue, MetricsFacade metrics, String name) {
        this.queue = queue;
        this.name = name;
        this.metrics = metrics;
        metrics.consumer().registerQueueUtilizationGauge(queue, name, q -> (double) q.size() / q.capacity());
    }

    @Override
    public boolean offer(T element) {
        boolean accepted = queue.offer(element);
        if (!accepted) {
            metrics.consumer().queueFailuresCounter(name).increment();
            logger.error("[Queue: {}] Unable to add item: queue is full. Offered item: {}", name, element);
        }
        return accepted;
    }

    @Override
    public void drain(MessagePassingQueue.Consumer<T> consumer) {
        queue.drain(consumer);
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public int capacity() {
        return queue.capacity();
    }
}
