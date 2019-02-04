package pl.allegro.tech.hermes.common.kafka;

import java.util.Objects;

public class KafkaTopicPartition {
    private final int partition;
    private final String topic;
    private final long currentOffset;
    private final long logEndOffset;
    private final long lag;
    private final String offsetMetadata;

    public KafkaTopicPartition(int partition, String topic, long currentOffset, long logEndOffset, String offsetMetadata) {
        this.partition = partition;
        this.topic = topic;
        this.currentOffset = currentOffset;
        this.logEndOffset = logEndOffset;
        this.offsetMetadata = offsetMetadata;

        this.lag = calculateLag(currentOffset, logEndOffset);
    }

    private long calculateLag(long currentOffset, long logEndOffset) {
        return logEndOffset - currentOffset;
    }

    public int getPartition() {
        return partition;
    }

    public String getTopic() {
        return topic;
    }

    public long getCurrentOffset() {
        return currentOffset;
    }

    public String getOffsetMetadata() {
        return offsetMetadata;
    }

    public long getLogEndOffset() {
        return logEndOffset;
    }

    public long getLag() {
        return lag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaTopicPartition that = (KafkaTopicPartition) o;
        return partition == that.partition &&
                currentOffset == that.currentOffset &&
                logEndOffset == that.logEndOffset &&
                lag == that.lag &&
                Objects.equals(topic, that.topic) &&
                Objects.equals(offsetMetadata, that.offsetMetadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partition, topic, currentOffset, logEndOffset, lag, offsetMetadata);
    }
}
