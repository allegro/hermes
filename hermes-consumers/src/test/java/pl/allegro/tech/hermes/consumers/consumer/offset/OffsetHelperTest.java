package pl.allegro.tech.hermes.consumers.consumer.offset;

import org.junit.Test;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OffsetHelperTest {
    OffsetHelper offsetHelper = new OffsetHelper();

    public static final String SOME_TOPIC = "topic";

    @Test
    public void shouldReturnNullForEmptyHelper() {
        //when
        Long offset = offsetHelper.getLastFullyRead();

        //then
        assertNull(offset);
    }

    @Test
    public void shouldReturnZeroWhenOffsetZeroIsFinished() {
        addOffsets(0, 2, 3);
        finishOffsets(0);

        //when
        Long offset = offsetHelper.getLastFullyRead();

        //then
        assertEquals(0, offset.longValue());
    }


    @Test
    public void shouldReturnZeroWhenOnlyOffsetZeroIsFinished() {
        addOffsets(0);
        finishOffsets(0);

        //when
        Long offset = offsetHelper.getLastFullyRead();

        //then
        assertEquals(0, offset.longValue());
    }


    @Test
    public void shouldReturnNullIfNoMessagesFinished() {
        //given
        addOffsets(1, 2, 3);

        //when
        Long offset = offsetHelper.getLastFullyRead();

        //then
        assertNull(offset);
    }

    @Test
    public void shouldReturnOffsetsIfInOrderFinishing() {
        //given
        addOffsets(1, 2, 3, 6);
        finishOffsets(1, 2, 3);

        //when
        Long offset = offsetHelper.getLastFullyRead();

        //then
        assertEquals(3, offset.longValue());
    }

    @Test
    public void shouldReturnOffsetsIfUnorderedFinishing() {
         //given
        addOffsets(1, 2, 3, 6);
        finishOffsets(1, 2, 6);

        //when
        Long offset = offsetHelper.getLastFullyRead();

        //then
        assertEquals(2, offset.longValue());
    }


    @Test
    public void shouldReturnPreviousIfDuplicatedNotRemoved() {
        //given
        addOffsets(1, 2, 2, 3);
        finishOffsets(1, 2, 3);

        //when
        Long offset = offsetHelper.getLastFullyRead();

        //then
        assertEquals(1, offset.longValue());
    }

    @Test
    public void shouldReturnOffsetsIfDuplicatedRemoved() {
        //given
        addOffsets(1, 2, 2, 3);
        finishOffsets(1, 2, 2);

        //when
        Long offset = offsetHelper.getLastFullyRead();

        //then
        assertEquals(2, offset.longValue());
    }

    @Test
    public void shouldReturnIfLastDeleted() {
        //given
        addOffsets(1, 2, 2);
        finishOffsets(1, 2, 2);

        //when
        Long offset = offsetHelper.getLastFullyRead();

        //then
        assertEquals(2, offset.longValue());

    }

    @Test
    public void shouldReturnIfGapFinished() {
        //given
        addOffsets(1, 2, 3, 4);
        finishOffsets(1, 4, 3, 2);

        //when
        Long offset = offsetHelper.getLastFullyRead();

        //then
        assertEquals(4, offset.longValue());

    }

    @Test
    public void shouldReturnNullIfDecrementBeforePut() {
        //given
        finishOffsets(1);
        addOffsets(1);

        //when
        Long offset = offsetHelper.getLastFullyRead();

        //then
        assertNull(offset);
    }

    private void addOffsets(long ... offsets) {
        for (long offset : offsets) {
            offsetHelper.put(new Message(Optional.of("id"), offset, 0, SOME_TOPIC, new byte[0], Optional.of(12091212L),
                    Optional.of(120912123L)));
        }
    }

    private void finishOffsets(long ... offsets) {
        for (long offset : offsets) {
            offsetHelper.decrement(offset);
        }
    }
}
