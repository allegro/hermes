package pl.allegro.tech.hermes.domain.subscription.offset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class PartitionOffset {

    private final int partition;
    private final long offset;

    @JsonCreator
    public PartitionOffset(@JsonProperty("offset") long offset, @JsonProperty("partition") int partition) {
        this.offset = offset;
        this.partition = partition;
    }

    public int getPartition() {
        return partition;
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PartitionOffset that = (PartitionOffset) obj;

        return Objects.equals(this.partition, that.partition)
                && Objects.equals(this.offset, that.offset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partition, offset);
    }

}