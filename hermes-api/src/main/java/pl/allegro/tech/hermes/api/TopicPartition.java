package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class TopicPartition {
  private final int partition;
  private final String topic;
  private final long currentOffset;
  private final long logEndOffset;
  private final long lag;
  private final String offsetMetadata;
  private final ContentType contentType;

  @JsonCreator
  public TopicPartition(
      @JsonProperty("partition") int partition,
      @JsonProperty("topic") String topic,
      @JsonProperty("currentOffset") long currentOffset,
      @JsonProperty("logEndOffset") long logEndOffset,
      @JsonProperty("offsetMetadata") String offsetMetadata,
      @JsonProperty("contentType") ContentType contentType) {
    this.partition = partition;
    this.topic = topic;
    this.currentOffset = currentOffset;
    this.logEndOffset = logEndOffset;
    this.offsetMetadata = offsetMetadata;
    this.contentType = contentType;

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

  public ContentType getContentType() {
    return contentType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TopicPartition that = (TopicPartition) o;
    return partition == that.partition
        && currentOffset == that.currentOffset
        && logEndOffset == that.logEndOffset
        && contentType == that.contentType
        && lag == that.lag
        && Objects.equals(topic, that.topic)
        && Objects.equals(offsetMetadata, that.offsetMetadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        partition, topic, currentOffset, logEndOffset, contentType, lag, offsetMetadata);
  }
}
