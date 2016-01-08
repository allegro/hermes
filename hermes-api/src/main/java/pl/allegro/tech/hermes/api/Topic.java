package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import pl.allegro.tech.hermes.api.helpers.Patch;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Objects;

public class Topic {
    @Valid @NotNull
    private TopicName name;

    private String description;

    private String messageSchema;

    private boolean validationEnabled;

    private boolean validationDryRunEnabled;

    private boolean jsonToAvroDryRunEnabled;

    private Ack ack;
    private ContentType contentType;

    public enum Ack {
        NONE, LEADER, ALL
    }

    @Valid
    private RetentionTime retentionTime;

    private boolean trackingEnabled;

    private boolean migratedFromJsonType;

    private Topic() { }

    public Topic(TopicName name, String description, RetentionTime retentionTime, String messageSchema,
                 boolean validationEnabled, boolean validationDryRunEnabled, boolean migratedFromJsonType, 
                 Ack ack, boolean trackingEnabled, ContentType contentType, boolean jsonToAvroDryRunEnabled) {
        this.name = name;
        this.description = description;
        this.retentionTime = retentionTime;
        this.messageSchema = messageSchema;
        this.validationEnabled = validationEnabled;
        this.validationDryRunEnabled = validationDryRunEnabled;
        this.ack = ack;
        this.trackingEnabled = trackingEnabled;
        this.migratedFromJsonType = migratedFromJsonType;
        this.contentType = contentType;
        this.jsonToAvroDryRunEnabled = jsonToAvroDryRunEnabled;
    }

    @JsonCreator
    public Topic(
            @JsonProperty("name") String qualifiedName,
            @JsonProperty("description") String description,
            @JsonProperty("retentionTime") RetentionTime retentionTime,
            @JsonProperty("messageSchema") String messageSchema,
            @JsonProperty("validation") boolean validationEnabled,
            @JsonProperty("validationDryRun") boolean validationDryRunEnabled,
            @JsonProperty("jsonToAvroDryRun") boolean jsonToAvroDryRunEnabled,
            @JsonProperty("ack") Ack ack,
            @JsonProperty("trackingEnabled") boolean trackingEnabled,
            @JsonProperty("migratedFromJsonType") boolean migratedFromJsonType,
            @JsonProperty("contentType") ContentType contentType) {

        this(TopicName.fromQualifiedName(qualifiedName), description, retentionTime, messageSchema, validationEnabled,
                validationDryRunEnabled, migratedFromJsonType, ack, trackingEnabled, contentType, jsonToAvroDryRunEnabled);
    }

    public RetentionTime getRetentionTime() {
        return retentionTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, retentionTime, messageSchema, validationEnabled, validationDryRunEnabled,
                migratedFromJsonType, trackingEnabled, ack, contentType, jsonToAvroDryRunEnabled);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Topic other = (Topic) obj;

        return Objects.equals(this.name, other.name)
            && Objects.equals(this.description, other.description)
            && Objects.equals(this.retentionTime, other.retentionTime)
            && Objects.equals(this.messageSchema, other.messageSchema)
            && Objects.equals(this.isValidationEnabled(), other.isValidationEnabled())
            && Objects.equals(this.validationDryRunEnabled, other.validationDryRunEnabled)
            && Objects.equals(this.jsonToAvroDryRunEnabled, other.jsonToAvroDryRunEnabled)
            && Objects.equals(this.trackingEnabled, other.trackingEnabled)
            && Objects.equals(this.migratedFromJsonType, other.migratedFromJsonType)
            && Objects.equals(this.ack, other.ack)
            && Objects.equals(this.contentType, other.contentType);
    }

    @JsonProperty("name")
    public String getQualifiedName() {
        return TopicName.toQualifiedName(name);
    }

    @JsonIgnore
    public TopicName getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRetentionTime(RetentionTime retentionTime) {
        this.retentionTime = retentionTime;
    }

    public String getMessageSchema() {
        return messageSchema;
    }

    @JsonProperty("validation")
    public boolean isValidationEnabled() {
        return validationEnabled || ContentType.AVRO == contentType;
    }

    @JsonProperty("validationDryRun")
    public boolean isValidationDryRunEnabled() {
        return validationDryRunEnabled;
    }

    @JsonProperty("jsonToAvroDryRun")
    public boolean isJsonToAvroDryRunEnabled() {
        return jsonToAvroDryRunEnabled;
    }

    public Ack getAck() {
        return ack;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public boolean isTrackingEnabled() {
        return trackingEnabled;
    }

    @JsonProperty("migratedFromJsonType")
    public boolean wasMigratedFromJsonType() {
        return migratedFromJsonType;
    }

    @JsonIgnore
    public boolean isReplicationConfirmRequired() {
        return getAck() == Ack.ALL;
    }

    public static class Builder {
        private Topic topic;

        public Builder() {
            topic = new Topic();
        }

        public Builder withName(String groupName, String topicName) {
            topic.name = new TopicName(groupName, topicName);
            return this;
        }

        public Builder withName(TopicName topicName) {
            topic.name = topicName;
            return this;
        }

        public Builder withName(String qualifiedName) {
            topic.name = TopicName.fromQualifiedName(qualifiedName);
            return this;
        }

        public Builder withDescription(String description) {
            topic.description = description;
            return this;
        }

        public Builder withRetentionTime(RetentionTime retentionTime) {
            topic.retentionTime = retentionTime;
            return this;
        }

        public Builder withRetentionTime(int retentionTime) {
            topic.retentionTime = new RetentionTime(retentionTime);
            return this;
        }

        public Builder withMessageSchema(String messageSchema) {
            topic.messageSchema = messageSchema;
            return this;
        }

        public Builder withValidation(boolean enabled) {
            topic.validationEnabled = enabled;
            return this;
        }

        public Builder withValidationDryRun(boolean enabled) {
            topic.validationDryRunEnabled = enabled;
            return this;
        }

        public Builder withJsonToAvroDryRun(boolean enabled) {
            topic.jsonToAvroDryRunEnabled = enabled;
            return this;
        }

        public Builder withAck(Ack ack) {
            topic.ack = ack;
            return this;
        }

        public Builder applyPatch(Object update) {
            if (update != null) {
                topic = Patch.apply(topic, update);
            }
            return this;
        }

        public Builder applyDefaults() {
            topic.retentionTime = new RetentionTime(1);
            topic.contentType = ContentType.JSON;
            return this;
        }

        public static Builder topic() {
            return new Builder();
        }

        public Builder withTrackingEnabled(boolean enabled) {
            topic.trackingEnabled = enabled;
            return this;
        }

        public Builder withContentType(ContentType contentType) {
            topic.contentType = contentType;
            return this;
        }

        public Builder migratedFromJsonType() {
            topic.migratedFromJsonType = true;
            return this;
        }

        public Topic build() {
            return topic;
        }
    }
}
