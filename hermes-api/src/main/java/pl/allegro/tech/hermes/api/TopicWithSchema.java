package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class TopicWithSchema extends Topic {

    private final Topic topic;

    private String schema;

    public TopicWithSchema(Topic topic, String schema) {
        this(schema, topic.getQualifiedName(), topic.getDescription(), topic.getOwner(), topic.getRetentionTime(),
                topic.isJsonToAvroDryRunEnabled(), topic.getAck(), topic.isFullTrackingEnabled(), topic.isErrorTrackingEnabled(),
                topic.wasMigratedFromJsonType(), topic.isSchemaVersionAwareSerializationEnabled(), topic.getContentType(), topic.getMaxMessageSize(),
                topic.getPublishingAuth(), topic.isSubscribingRestricted(), topic.getOfflineStorage());
    }

    @JsonCreator
    public TopicWithSchema(@JsonProperty("schema") String schema,
                           @JsonProperty("name") String qualifiedName,
                           @JsonProperty("description") String description,
                           @JsonProperty("owner") OwnerId owner,
                           @JsonProperty("retentionTime") RetentionTime retentionTime,
                           @JsonProperty("jsonToAvroDryRun") boolean jsonToAvroDryRunEnabled,
                           @JsonProperty("ack") Ack ack,
                           @JsonProperty("trackingEnabled") boolean trackingEnabled,
                           @JsonProperty("errorTrackingEnabled") boolean errorTrackingEnabled,
                           @JsonProperty("migratedFromJsonType") boolean migratedFromJsonType,
                           @JsonProperty("schemaVersionAwareSerializationEnabled") boolean schemaVersionAwareSerializationEnabled,
                           @JsonProperty("contentType") ContentType contentType,
                           @JsonProperty("maxMessageSize") Integer maxMessageSize,
                           @JsonProperty("auth") PublishingAuth publishingAuth,
                           @JsonProperty("subscribingRestricted") boolean subscribingRestricted,
                           @JsonProperty("offlineStorage") TopicDataOfflineStorage offlineStorage) {
        super(qualifiedName, description, owner, retentionTime, jsonToAvroDryRunEnabled, ack, trackingEnabled, errorTrackingEnabled,
                migratedFromJsonType, schemaVersionAwareSerializationEnabled, contentType, maxMessageSize,
                publishingAuth, subscribingRestricted, offlineStorage);
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
        return new Topic(this.getQualifiedName(), this.getDescription(), this.getOwner(), this.getRetentionTime(),
                this.isJsonToAvroDryRunEnabled(), this.getAck(), this.isFullTrackingEnabled(), this.isErrorTrackingEnabled(),
                this.wasMigratedFromJsonType(), this.isSchemaVersionAwareSerializationEnabled(), this.getContentType(), this.getMaxMessageSize(),
                this.getPublishingAuth(), this.isSubscribingRestricted(), this.getOfflineStorage());
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
        return Objects.equals(topic, that.topic) &&
                Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), topic, schema);
    }
}
