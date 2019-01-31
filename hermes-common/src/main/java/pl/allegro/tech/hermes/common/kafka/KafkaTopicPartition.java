package pl.allegro.tech.hermes.common.kafka;

import java.util.Objects;

public class KafkaTopicPartition {
    private final int partition;
    private final String topic;
    private final long offset;
    private final String offsetMetadata;

    public KafkaTopicPartition(int partition, String topic, long offset, String offsetMetadata) {
        this.partition = partition;
        this.topic = topic;
        this.offset = offset;
        this.offsetMetadata = offsetMetadata;
    }

    public int getPartition() {
        return partition;
    }

    public String getTopic() {
        return topic;
    }

    public long getOffset() {
        return offset;
    }

    public String getOffsetMetadata() {
        return offsetMetadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaTopicPartition that = (KafkaTopicPartition) o;
        return partition == that.partition &&
                offset == that.offset &&
                Objects.equals(topic, that.topic) &&
                Objects.equals(offsetMetadata, that.offsetMetadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partition, topic, offset, offsetMetadata);
    }
}
