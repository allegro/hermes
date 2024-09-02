package pl.allegro.tech.hermes.consumers.queue;

import org.jctools.queues.MessagePassingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.metrics.HermesCounter;

public class MonitoredMpscQueue<T> implements MpscQueue<T> {

  private static final Logger logger = LoggerFactory.getLogger(MonitoredMpscQueue.class);

  private final MpscQueue<T> queue;

  private final String name;

  private final HermesCounter failuresCounter;

  public MonitoredMpscQueue(MpscQueue<T> queue, MetricsFacade metrics, String name) {
    this.queue = queue;
    this.name = name;
    metrics
        .consumer()
        .registerQueueUtilizationGauge(queue, name, q -> (double) q.size() / q.capacity());
    this.failuresCounter = metrics.consumer().queueFailuresCounter(name);
  }

  @Override
  public boolean offer(T element) {
    boolean accepted = queue.offer(element);
    if (!accepted) {
      failuresCounter.increment();
      logger.error(
          "[Queue: {}] Unable to add item: queue is full. Offered item: {}", name, element);
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
