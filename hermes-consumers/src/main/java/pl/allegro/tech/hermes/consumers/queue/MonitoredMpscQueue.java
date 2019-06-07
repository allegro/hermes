package pl.allegro.tech.hermes.consumers.queue;

import org.jctools.queues.MessagePassingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

public class MonitoredMpscQueue<T> implements MpscQueue<T> {

    private static final Logger logger = LoggerFactory.getLogger(MonitoredMpscQueue.class);

    private final MpscQueue<T> queue;

    private final String name;

    private final HermesMetrics metrics;

    public MonitoredMpscQueue(MpscQueue<T> queue, HermesMetrics metrics, String name) {
        this.queue = queue;
        this.name = name;
        this.metrics = metrics;
        metrics.registerGauge("queue." + name + ".utilization", () -> (double) queue.size() / queue.capacity());
    }

    @Override
    public boolean offer(T element) {
        boolean accepted = queue.offer(element);
        if (!accepted) {
            metrics.counter("queue." + name + ".failures").inc();
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
