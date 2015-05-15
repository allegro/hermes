package pl.allegro.tech.hermes.consumers.consumer.offset;

import org.junit.Test;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;
import pl.allegro.tech.hermes.consumers.test.Wait;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class PartitionOffsetHelperTest {

    private static final int FIRST_PARTITION = 0;
    private static final int SECOND_PARTITION = 1;
    public static final String SOME_TOPIC = "topic";

    PartitionOffsetHelper offsetHelper = new PartitionOffsetHelper(100);

    @Test
    public void shouldReturnNullForEmptyHelper() {
        //when
        List<PartitionOffset> offsets = offsetHelper.getAllLastFullyRead();

        //then
        assertThat(offsets).isEmpty();
    }

    @Test
    public void shouldReturnZeroWhenOnlyOffsetZeroIsFinished() {
        //given
        partition(FIRST_PARTITION).addOffsets(0).finishOffsets(0);

        //when
        List<PartitionOffset> offsets = offsetHelper.getAllLastFullyRead();

        // then
        assertThat(offsets).containsOnlyOnce(new PartitionOffset(0, FIRST_PARTITION));
    }

    @Test
    public void shouldReturnNullIfNoMessagesFinished() {
        //given
        partition(FIRST_PARTITION).addOffsets(1, 2, 3);

        //when
        List<PartitionOffset> offsets = offsetHelper.getAllLastFullyRead();

        //then
        assertThat(offsets).isEmpty();
    }

    @Test
    public void shouldReturnLastFinishedOffset() {
        // given
        partition(FIRST_PARTITION).addOffsets(1).finishOffsets(1);

        // when
        List<PartitionOffset> offsets = offsetHelper.getAllLastFullyRead();

        // then
        assertThat(offsets).containsOnly(new PartitionOffset(1, FIRST_PARTITION));
    }

    @Test
    public void shouldReturnLastFinishedOffsetForCorrectPartition() {
        // given
        partition(FIRST_PARTITION).addOffsets(1).finishOffsets(1);
        partition(SECOND_PARTITION).addOffsets(2).finishOffsets(2);

        // when
        List<PartitionOffset> offsets = offsetHelper.getAllLastFullyRead();

        // then
        assertThat(offsets).containsExactly(new PartitionOffset(1, FIRST_PARTITION), new PartitionOffset(2, SECOND_PARTITION));
    }

    @Test
    public void shouldNotCommitUnchangedOffset() {
        //given
        partition(FIRST_PARTITION).addOffsets(1).finishOffsets(1);

        //when
        offsetHelper.getAllLastFullyRead();
        Wait.forCacheInvalidation();
        List<PartitionOffset> offsets = offsetHelper.getAllLastFullyRead();

        //then
        assertThat(offsets).isEmpty();
    }

    @Test
    public void shouldCommitWhenOffsetFinallyChanges() {
        //given
        Partition partition = partition(FIRST_PARTITION);
        partition.addOffsets(1).finishOffsets(1);

        //when
        offsetHelper.getAllLastFullyRead();
        Wait.forCacheInvalidation();
        partition.addOffsets(2).finishOffsets(2);


        //then
        List<PartitionOffset> offsets = offsetHelper.getAllLastFullyRead();
        assertThat(offsets).containsOnly(new PartitionOffset(2, FIRST_PARTITION));
    }

    private Partition partition(final int partition) {
        return new Partition(partition);
    }

    private class Partition {
        private final int partition;

        public Partition(int partition) {

            this.partition = partition;
        }

        public Partition addOffsets(long... offsets) {
            for (long offset : offsets) {
                offsetHelper.put(new Message(Optional.of("id"), offset, partition, SOME_TOPIC, new byte[partition],
                        Optional.of(213123L), Optional.of(2131234L)));
            }
            return this;
        }

        private Partition finishOffsets(long... offsets) {
            for (long offset : offsets) {
                offsetHelper.decrement(partition, offset);
            }
            return this;
        }
    }
}
