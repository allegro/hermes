package pl.allegro.tech.hermes.common.kafka.offset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;

public class PartitionOffset {

  private final KafkaTopicName topic;
  private final int partition;
  private final long offset;

  @JsonCreator
  public PartitionOffset(
      @JsonProperty("topic") KafkaTopicName topic,
      @JsonProperty("offset") long offset,
      @JsonProperty("partition") int partition) {
    this.topic = topic;
    this.offset = offset;
    this.partition = partition;
  }

  public KafkaTopicName getTopic() {
    return topic;
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

    return Objects.equals(this.topic, that.topic)
        && Objects.equals(this.partition, that.partition)
        && Objects.equals(this.offset, that.offset);
  }

  @Override
  public int hashCode() {
    return Objects.hash(topic, partition, offset);
  }

  @Override
  public String toString() {
    return "PartitionOffset{"
        + "topic="
        + topic
        + ", partition="
        + partition
        + ", offset="
        + offset
        + '}';
  }
}
