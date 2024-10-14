package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public class TopicWithSchema extends Topic {

  private final Topic topic;

  private String schema;

  public TopicWithSchema(Topic topic, String schema) {
    this(
        schema,
        topic.getQualifiedName(),
        topic.getDescription(),
        topic.getOwner(),
        topic.getRetentionTime(),
        topic.isJsonToAvroDryRunEnabled(),
        topic.getAck(),
        topic.isFallbackToRemoteDatacenterEnabled(),
        topic.getChaos(),
        topic.isTrackingEnabled(),
        topic.wasMigratedFromJsonType(),
        topic.isSchemaIdAwareSerializationEnabled(),
        topic.getContentType(),
        topic.getMaxMessageSize(),
        topic.getPublishingAuth(),
        topic.isSubscribingRestricted(),
        topic.getOfflineStorage(),
        topic.getLabels(),
        topic.getCreatedAt(),
        topic.getModifiedAt());
  }

  @JsonCreator
  public TopicWithSchema(
      @JsonProperty("schema") String schema,
      @JsonProperty("name") String qualifiedName,
      @JsonProperty("description") String description,
      @JsonProperty("owner") OwnerId owner,
      @JsonProperty("retentionTime") RetentionTime retentionTime,
      @JsonProperty("jsonToAvroDryRun") boolean jsonToAvroDryRunEnabled,
      @JsonProperty("ack") Ack ack,
      @JsonProperty("fallbackToRemoteDatacenterEnabled")
          @JacksonInject(
              value = DEFAULT_FALLBACK_TO_REMOTE_DATACENTER_KEY,
              useInput = OptBoolean.TRUE)
          boolean fallbackToRemoteDatacenterEnabled,
      @JsonProperty("chaos") PublishingChaosPolicy chaos,
      @JsonProperty("trackingEnabled") boolean trackingEnabled,
      @JsonProperty("migratedFromJsonType") boolean migratedFromJsonType,
      @JsonProperty("schemaIdAwareSerializationEnabled")
          @JacksonInject(
              value = Topic.DEFAULT_SCHEMA_ID_SERIALIZATION_ENABLED_KEY,
              useInput = OptBoolean.TRUE)
          Boolean schemaIdAwareSerializationEnabled,
      @JsonProperty("contentType") ContentType contentType,
      @JsonProperty("maxMessageSize") Integer maxMessageSize,
      @JsonProperty("auth") PublishingAuth publishingAuth,
      @JsonProperty("subscribingRestricted") boolean subscribingRestricted,
      @JsonProperty("offlineStorage") TopicDataOfflineStorage offlineStorage,
      @JsonProperty("labels") Set<TopicLabel> labels,
      @JsonProperty("createdAt") Instant createdAt,
      @JsonProperty("modifiedAt") Instant modifiedAt) {
    super(
        qualifiedName,
        description,
        owner,
        retentionTime,
        jsonToAvroDryRunEnabled,
        ack,
        fallbackToRemoteDatacenterEnabled,
        chaos,
        trackingEnabled,
        migratedFromJsonType,
        schemaIdAwareSerializationEnabled,
        contentType,
        maxMessageSize,
        publishingAuth,
        subscribingRestricted,
        offlineStorage,
        labels,
        createdAt,
        modifiedAt);
    this.topic = convertToTopic();
    this.schema = schema;
  }

  public static TopicWithSchema topicWithSchema(Topic topic, String schema) {
    return new TopicWithSchema(topic, schema);
  }

  public static TopicWithSchema topicWithSchema(Topic topic) {
    return new TopicWithSchema(topic, null);
  }

  private Topic convertToTopic() {
    return new Topic(
        this.getQualifiedName(),
        this.getDescription(),
        this.getOwner(),
        this.getRetentionTime(),
        this.isJsonToAvroDryRunEnabled(),
        this.getAck(),
        this.isFallbackToRemoteDatacenterEnabled(),
        this.getChaos(),
        this.isTrackingEnabled(),
        this.wasMigratedFromJsonType(),
        this.isSchemaIdAwareSerializationEnabled(),
        this.getContentType(),
        this.getMaxMessageSize(),
        this.getPublishingAuth(),
        this.isSubscribingRestricted(),
        this.getOfflineStorage(),
        this.getLabels(),
        this.getCreatedAt(),
        this.getModifiedAt());
  }

  public String getSchema() {
    return schema;
  }

  @JsonIgnore
  public Topic getTopic() {
    return topic;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    TopicWithSchema that = (TopicWithSchema) o;
    return Objects.equals(topic, that.topic) && Objects.equals(schema, that.schema);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), topic, schema);
  }
}
