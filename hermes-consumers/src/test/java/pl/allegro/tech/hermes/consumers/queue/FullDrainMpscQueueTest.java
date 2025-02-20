package pl.allegro.tech.hermes.consumers.queue;

public class FullDrainMpscQueueTest extends MpscQueuesAbstractTest {

  @Override
  protected <T> MpscQueue<T> createMpscQueue(int size) {
    return new FullDrainMpscQueue<>(size);
  }
}
