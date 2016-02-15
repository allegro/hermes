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
    public SentMessageTrace(@JsonProperty("messageId") String messageId,
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

    public static SentMessageTrace createUndeliveredMessage(Subscription subscription, String message,
            Throwable cause, Long loggingTime, Integer partition, Long offset, String cluster) {
        return createUndeliveredMessage(subscription.getQualifiedTopicName(), subscription.getName(), message,
                cause.getMessage(), loggingTime, partition, offset, cluster);
    }

    public static SentMessageTrace createUndeliveredMessage(String qualifiedTopicName, String subscription, String message,
            String cause, Long loggingTime, Integer partition, Long offset, String cluster) {
        return new SentMessageTrace(
                null,
                null,
                loggingTime,
                subscription,
                qualifiedTopicName,
                SentMessageTraceStatus.DISCARDED,
                cause,
                message,
                partition,
                offset,
                cluster
        );
    }

    public static SentMessageTrace createUndeliveredMessage(TopicName topicName, String subscription, String message,
                                                                 Throwable cause, Long loggingTime, Integer partition, Long offset, String cluster) {
        return new SentMessageTrace(
                null,
                null,
                loggingTime,
                subscription,
                topicName.qualifiedName(),
                SentMessageTraceStatus.DISCARDED,
                cause.getMessage(),
                message,
                partition,
                offset,
                cluster
        );
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
}
