package pl.allegro.tech.hermes.common.kafka.offset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;

import java.util.Objects;

public class PartitionOffset {

    private final KafkaTopic topic;
    private final int partition;
    private final long offset;

    @JsonCreator
    public PartitionOffset(@JsonProperty("topic") KafkaTopic topic,
                           @JsonProperty("offset") long offset,
                           @JsonProperty("partition") int partition) {
        this.topic = topic;
        this.offset = offset;
        this.partition = partition;
    }

    public KafkaTopic getTopic() {
        return topic;
    }

    public int getPartition() {
        return partition;
    }

    public long getOffset() {
        return offset;
    }

    public PartitionOffset withIncrementedOffset() {
        return new PartitionOffset(topic, offset + 1, partition);
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

        return Objects.equals(this.topic, that.topic)
                && Objects.equals(this.partition, that.partition)
                && Objects.equals(this.offset, that.offset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, partition, offset);
    }

}