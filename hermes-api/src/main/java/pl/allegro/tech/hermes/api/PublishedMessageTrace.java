package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishedMessageTrace implements MessageTrace {

  private final String messageId;
  private final long timestamp;
  private final PublishedMessageTraceStatus status;
  private final TopicName topicName;
  private final String reason;
  private final String message;
  private final String cluster;
  private final String extraRequestHeaders;
  private final String storageDatacenter;

  @JsonCreator
  public PublishedMessageTrace(
      @JsonProperty("messageId") String messageId,
      @JsonProperty("timestamp") Long timestamp,
      @JsonProperty("topicName") String topicName,
      @JsonProperty("status") PublishedMessageTraceStatus status,
      @JsonProperty("reason") String reason,
      @JsonProperty("message") String message,
      @JsonProperty("cluster") String cluster,
      @JsonProperty("extraRequestHeaders") String extraRequestHeaders,
      @JsonProperty("storageDc") String storageDatacenter) {
    this.messageId = messageId;
    this.timestamp = timestamp;
    this.status = status;
    this.topicName = TopicName.fromQualifiedName(topicName);
    this.reason = reason;
    this.message = message;
    this.cluster = cluster;
    this.extraRequestHeaders = extraRequestHeaders;
    this.storageDatacenter = storageDatacenter;
  }

  public String getMessageId() {
    return messageId;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public String getReason() {
    return reason;
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

  public PublishedMessageTraceStatus getStatus() {
    return status;
  }

  public String getCluster() {
    return cluster;
  }

  public String getExtraRequestHeaders() {
    return extraRequestHeaders;
  }

  @JsonProperty("storageDc")
  public String getStorageDatacenter() {
    return storageDatacenter;
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
    final PublishedMessageTrace other = (PublishedMessageTrace) obj;
    return Objects.equals(this.messageId, other.messageId);
  }
}
