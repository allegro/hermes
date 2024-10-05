package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SentMessageTrace implements MessageTrace {

  private final String messageId;
  private final String batchId;
  private final String subscription;
  private final long timestamp;
  private final SentMessageTraceStatus status;
  private final Integer partition;
  private final Long offset;
  private final TopicName topicName;
  private final String reason;
  private final String message;
  private final String cluster;

  @JsonCreator
  public SentMessageTrace(
      @JsonProperty("messageId") String messageId,
      @JsonProperty("batchId") String batchId,
      @JsonProperty("timestamp") Long timestamp,
      @JsonProperty("subscription") String subscription,
      @JsonProperty("topicName") String topicName,
      @JsonProperty("status") SentMessageTraceStatus status,
      @JsonProperty("reason") String reason,
      @JsonProperty("message") String message,
      @JsonProperty("partition") Integer partition,
      @JsonProperty("offset") Long offset,
      @JsonProperty("cluster") String cluster) {
    this.messageId = messageId;
    this.batchId = batchId;
    this.timestamp = timestamp;
    this.subscription = subscription;
    this.status = status;
    this.partition = partition;
    this.offset = offset;
    this.topicName = TopicName.fromQualifiedName(topicName);
    this.reason = reason;
    this.message = message;
    this.cluster = cluster;
  }

  public String getMessageId() {
    return messageId;
  }

  public String getBatchId() {
    return batchId;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public String getSubscription() {
    return subscription;
  }

  public String getReason() {
    return reason;
  }

  public Integer getPartition() {
    return partition;
  }

  public Long getOffset() {
    return offset;
  }

  @JsonProperty("topicName")
  public String getQualifiedTopicName() {
    return TopicName.toQualifiedName(topicName);
  }

  @JsonIgnore
  public TopicName getTopicName() {
    return topicName;
  }

  public String getMessage() {
    return message;
  }

  public SentMessageTraceStatus getStatus() {
    return status;
  }

  public String getCluster() {
    return cluster;
  }

  @Override
  public int hashCode() {
    return Objects.hash(messageId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final SentMessageTrace other = (SentMessageTrace) obj;
    return Objects.equals(this.messageId, other.messageId)
        && Objects.equals(this.subscription, other.subscription)
        && Objects.equals(this.timestamp, other.timestamp)
        && Objects.equals(this.status, other.status);
  }

  public static class Builder {

    private final String messageId;
    private final String batchId;
    private final SentMessageTraceStatus status;

    private String subscription;
    private long timestamp;
    private Integer partition;
    private Long offset;
    private String topicName;
    private String reason;
    private String message;
    private String cluster;

    private Builder(String messageId, String batchId, SentMessageTraceStatus status) {
      this.messageId = messageId;
      this.batchId = batchId;
      this.status = status;
    }

    public Builder withSubscription(String subscription) {
      this.subscription = subscription;
      return this;
    }

    public Builder withTimestamp(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder withPartition(Integer partition) {
      this.partition = partition;
      return this;
    }

    public Builder withOffset(Long offset) {
      this.offset = offset;
      return this;
    }

    public Builder withTopicName(String topicName) {
      this.topicName = topicName;
      return this;
    }

    public Builder withReason(String reason) {
      this.reason = reason;
      return this;
    }

    public Builder withMessage(String message) {
      this.message = message;
      return this;
    }

    public Builder withCluster(String cluster) {
      this.cluster = cluster;
      return this;
    }

    public static Builder sentMessageTrace(
        String messageId, String batchId, SentMessageTraceStatus status) {
      return new Builder(messageId, batchId, status);
    }

    public static Builder undeliveredMessage() {
      return new Builder(null, null, SentMessageTraceStatus.DISCARDED);
    }

    public SentMessageTrace build() {
      return new SentMessageTrace(
          messageId,
          batchId,
          timestamp,
          subscription,
          topicName,
          status,
          reason,
          message,
          partition,
          offset,
          cluster);
    }
  }
}
