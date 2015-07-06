package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import pl.allegro.tech.hermes.api.helpers.Patch;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.function.Function;

import static com.google.common.base.Strings.isNullOrEmpty;

public class Topic {
    @Valid @NotNull
    private TopicName name;

    private String description;

    private String messageSchema;

    private Object compiledSchema;

    private boolean validationEnabled;

    private Ack ack;

    private ContentType contentType;

    public enum Ack {
        NONE, LEADER, ALL
    }

    public enum ContentType {
        JSON, AVRO
    }

    @Valid
    private RetentionTime retentionTime;

    private boolean trackingEnabled;

    private Topic() { }

    public Topic(TopicName name, String description, RetentionTime retentionTime, String messageSchema,
                 boolean validationEnabled, Ack ack, boolean trackingEnabled, ContentType contentType) {
        this.name = name;
        this.description = description;
        this.retentionTime = retentionTime;
        this.messageSchema = messageSchema;
        this.validationEnabled = validationEnabled;
        this.ack = ack;
        this.trackingEnabled = trackingEnabled;
        this.contentType = contentType;
    }

    @JsonCreator
    public Topic(
            @JsonProperty("name") String qualifiedName,
            @JsonProperty("description") String description,
            @JsonProperty("retentionTime") RetentionTime retentionTime,
            @JsonProperty("messageSchema") String messageSchema,
            @JsonProperty("validation") boolean validationEnabled,
            @JsonProperty("ack") Ack ack,
            @JsonProperty("trackingEnabled") boolean trackingEnabled,
            @JsonProperty("contentType") ContentType contentType) {

        this(TopicName.fromQualifiedName(qualifiedName), description, retentionTime, messageSchema, validationEnabled, ack,
             trackingEnabled, contentType);
    }

    public RetentionTime getRetentionTime() {
        return retentionTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, retentionTime, messageSchema, validationEnabled, trackingEnabled, ack, contentType);
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
            && Objects.equals(this.validationEnabled, other.validationEnabled)
            && Objects.equals(this.trackingEnabled, other.trackingEnabled)
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

    @JsonIgnore
    public <T> T getCompiledSchema(Function<String, T> compiler) {
        if (compiledSchema == null) {
            compiledSchema = compiler.apply(getMessageSchema());
        }
        return (T) compiledSchema;
    }

    @JsonProperty("validation")
    public boolean isValidationEnabled() {
        return validationEnabled || ContentType.AVRO == contentType;
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

    @JsonIgnore
    public boolean isReplicationConfirmRequired() {
        return getAck() == Ack.ALL;
    }

    @JsonIgnore
    public boolean isSchemaValidationRequired() {
        return Topic.ContentType.AVRO == contentType || !isNullOrEmpty(messageSchema);
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

        public Topic build() {
            return topic;
        }
    }
}
