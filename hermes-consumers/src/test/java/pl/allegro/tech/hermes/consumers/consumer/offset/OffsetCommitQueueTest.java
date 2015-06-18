package pl.allegro.tech.hermes.consumers.consumer.offset;

import org.junit.Test;
import org.mockito.Mockito;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class OffsetCommitQueueTest {

    private OffsetCommitQueueMonitor monitor = Mockito.mock(OffsetCommitQueueMonitor.class);

    private OffsetCommitQueue queue = new OffsetCommitQueue(monitor);

    @Test
    public void shouldReturnNullForEmptyHelper() {
        //when
        Optional<Long> offset = queue.poll();

        //then
        assertThat(offset.isPresent()).isFalse();
    }

    @Test
    public void shouldReturnZeroWhenOffsetZeroIsFinished() {
        addOffsets(0, 2, 3);
        finishOffsets(0);

        //when
        Optional<Long> offset = queue.poll();

        //then
        assertThat(offset.get()).isZero();
    }


    @Test
    public void shouldReturnZeroWhenOnlyOffsetZeroIsFinished() {
        addOffsets(0);
        finishOffsets(0);

        //when
        Optional<Long> offset = queue.poll();

        //then
        assertThat(offset.get()).isZero();
    }


    @Test
    public void shouldReturnNullIfNoMessagesFinished() {
        //given
        addOffsets(1, 2, 3);

        //when
        Optional<Long> offset = queue.poll();

        //then
        assertThat(offset.isPresent()).isFalse();
    }

    @Test
    public void shouldReturnOffsetsIfInOrderFinishing() {
        //given
        addOffsets(1, 2, 3, 6);
        finishOffsets(1, 2, 3);

        //when
        Optional<Long> offset = queue.poll();

        //then
        assertThat(offset.get()).isEqualTo(3);
    }

    @Test
    public void shouldReturnOffsetsIfUnorderedFinishing() {
         //given
        addOffsets(1, 2, 3, 6);
        finishOffsets(1, 2, 6);

        //when
        Optional<Long> offset = queue.poll();

        //then
        assertThat(offset.get()).isEqualTo(2);
    }

    @Test
    public void shouldReturnIfLastDeleted() {
        //given
        addOffsets(1, 2, 2);
        finishOffsets(1, 2, 2);

        //when
        Optional<Long> offset = queue.poll();

        //then
        assertThat(offset.get()).isEqualTo(2);
    }

    @Test
    public void shouldReturnIfGapFinished() {
        //given
        addOffsets(1, 2, 3, 4);
        finishOffsets(1, 4, 3, 2);

        //when
        Optional<Long> offset = queue.poll();

        //then
        assertThat(offset.get()).isEqualTo(4);
    }

    @Test
    public void shouldReturnNullIfDecrementBeforePut() {
        //given
        finishOffsets(1);
        addOffsets(1);

        //when
        Optional<Long> offset = queue.poll();

        //then
        assertThat(offset.isPresent()).isFalse();
    }

    @Test
    public void shouldNotSkipHolesInOffsetSequence() {
        //given
        addOffsets(1,2,6,7);
        finishOffsets(1,2,6,7);

        //when & then
        assertThat(queue.poll().get()).isEqualTo(7);
    }

    @Test
    public void shouldSetLastToCommitToNullWhenAllMessagesAreDelivered() {
        //given
        addOffsets(1,2,3);
        finishOffsets(3,1,2);
        addOffsets(4,5,6,7);
        finishOffsets(7,4,5,6);

        // when & then
        assertThat(queue.poll().get()).isEqualTo(7);
        assertThat(queue.poll().isPresent()).isFalse();
    }


    private void addOffsets(long ... offsets) {
        for (Long offset : offsets) {
            queue.put(new Message("id", offset, 0, "topic", new byte[0], 12091212L, 120912123L).getOffset());
        }
    }

    private void finishOffsets(long ... offsets) {
        for (Long offset : offsets) {
            queue.markDelivered(offset);
        }
    }
}
