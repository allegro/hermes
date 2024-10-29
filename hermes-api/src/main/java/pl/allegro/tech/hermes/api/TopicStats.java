package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class TopicStats {
  private final long topicCount;
  private final long ackAllTopicCount;
  private final long trackingEnabledTopicCount;
  private final long avroTopicCount;

  @JsonCreator
  public TopicStats(
      @JsonProperty("topicCount") long topicCount,
      @JsonProperty("ackAllTopicCount") long ackAllTopicCount,
      @JsonProperty("trackingEnabledTopicCount") long trackingEnabledTopicCount,
      @JsonProperty("avroTopicCount") long avroTopicCount) {
    this.topicCount = topicCount;
    this.ackAllTopicCount = ackAllTopicCount;
    this.trackingEnabledTopicCount = trackingEnabledTopicCount;
    this.avroTopicCount = avroTopicCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TopicStats that = (TopicStats) o;
    return topicCount == that.topicCount
        && ackAllTopicCount == that.ackAllTopicCount
        && trackingEnabledTopicCount == that.trackingEnabledTopicCount
        && avroTopicCount == that.avroTopicCount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(topicCount, ackAllTopicCount, trackingEnabledTopicCount, avroTopicCount);
  }

  @Override
  public String toString() {
    return "TopicStats{"
        + "topicCount="
        + topicCount
        + ", ackAllTopicCount="
        + ackAllTopicCount
        + ", trackingEnabledTopicCount="
        + trackingEnabledTopicCount
        + ", avroTopicCount="
        + avroTopicCount
        + '}';
  }

  public long getTopicCount() {
    return topicCount;
  }

  public long getAckAllTopicCount() {
    return ackAllTopicCount;
  }

  public long getTrackingEnabledTopicCount() {
    return trackingEnabledTopicCount;
  }

  public long getAvroTopicCount() {
    return avroTopicCount;
  }
}
