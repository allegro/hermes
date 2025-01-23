package pl.allegro.tech.hermes.consumers.queue;

import static org.slf4j.LoggerFactory.getLogger;

import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscArrayQueue;
import org.slf4j.Logger;

public class FullDrainMpscQueue<T> implements MpscQueue<T> {

  private static final Logger logger = getLogger(FullDrainMpscQueue.class);

  private final MpscArrayQueue<T> queue;

  public FullDrainMpscQueue(int capacity) {
    this.queue = new MpscArrayQueue<>(capacity);
  }

  @Override
  public boolean offer(T element) {
    return queue.offer(element);
  }

  /**
   * The {@link MpscArrayQueue#drain(MessagePassingQueue.Consumer)} method may skip items with
   * allocated slots by producers (who won CAS) but were not added to the queue yet. This may happen
   * to broken elements chain. See explanation <a
   * href="http://psy-lob-saw.blogspot.com/2014/07/poll-me-maybe.html">here</a>.
   *
   * <p>This is an alternative approach which waits for all items to become available by using
   * {@link MpscArrayQueue#poll()} underneath (which spin-waits when getting next item).
   */
  @Override
  public void drain(MessagePassingQueue.Consumer<T> consumer) {
    int size = queue.size();
    for (int i = 0; i < size; i++) {
      T element = queue.poll();
      if (element != null) {
        consumer.accept(element);
      } else {
        logger.warn("Unexpected null value while draining queue [idx={}, size={}]", i, size);
        break;
      }
    }
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
