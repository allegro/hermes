package pl.allegro.tech.hermes.consumers.queue;

import com.codahale.metrics.Gauge;
import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

public class MonitoredMpscQueue<T> {

    private static final Logger logger = LoggerFactory.getLogger(MonitoredMpscQueue.class);

    private final MpscArrayQueue<T> queue;

    private final String name;

    public MonitoredMpscQueue(HermesMetrics metrics, String name, int capacity) {
        this.queue = new MpscArrayQueue<>(capacity);
        this.name = name;

        metrics.registerGauge("queue." + name + ".utilization", () -> queue.size() / queue.capacity());
    }

    public boolean offer(T element) {
        boolean accepted = queue.offer(element);
        if (!accepted) {
            logger.error("[Queue: {}] Unable to add item: queue is full. Offered item: {}", name, element);
        }
        return accepted;
    }

    public void drain(MessagePassingQueue.Consumer<T> consumer) {
        queue.drain(consumer);
    }

}
