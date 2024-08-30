package pl.allegro.tech.hermes.consumers.queue;

public class WaitFreeDrainMpscQueueTest extends MpscQueuesAbstractTest {

  @Override
  protected <T> MpscQueue<T> createMpscQueue(int size) {
    return new WaitFreeDrainMpscQueue<>(size);
  }
}
