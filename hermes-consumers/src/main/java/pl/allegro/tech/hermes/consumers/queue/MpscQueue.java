package pl.allegro.tech.hermes.consumers.queue;

import org.jctools.queues.MessagePassingQueue;

public interface MpscQueue<T> {

  boolean offer(T element);

  void drain(MessagePassingQueue.Consumer<T> consumer);

  int size();

  int capacity();
}
