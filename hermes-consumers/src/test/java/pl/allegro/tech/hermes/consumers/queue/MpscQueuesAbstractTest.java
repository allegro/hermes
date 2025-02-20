package pl.allegro.tech.hermes.consumers.queue;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public abstract class MpscQueuesAbstractTest {

  protected abstract <T> MpscQueue<T> createMpscQueue(int size);

  @Test
  public void shouldDrainItemsFromNonEmptyQueue() {
    // given
    MpscQueue<Integer> queue = createMpscQueue(16);
    assertThat(queue.capacity()).isEqualTo(16);

    // when
    queue.offer(1);
    queue.offer(2);
    queue.offer(3);

    // then
    assertThat(queue.size()).isEqualTo(3);

    // when
    List<Integer> drained = new ArrayList<>();
    queue.drain(drained::add);

    // then
    assertThat(drained).contains(1, 2, 3);

    // and
    assertThat(queue.size()).isZero();
  }

  @Test
  public void shouldDrainEmptyQueue() {
    // given
    MpscQueue<Integer> queue = createMpscQueue(16);

    // when
    List<Integer> drained = new ArrayList<>();
    queue.drain(drained::add);

    // then
    assertThat(drained).isEmpty();
  }
}
