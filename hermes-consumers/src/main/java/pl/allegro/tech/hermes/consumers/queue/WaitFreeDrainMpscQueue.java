package pl.allegro.tech.hermes.consumers.queue;

import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscArrayQueue;

public class WaitFreeDrainMpscQueue<T> implements MpscQueue<T> {

  private final MpscArrayQueue<T> queue;

  public WaitFreeDrainMpscQueue(int capacity) {
    this.queue = new MpscArrayQueue<>(capacity);
  }

  @Override
  public boolean offer(T element) {
    return queue.offer(element);
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
