package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Objects;

public class Topic {

    @Valid
    @NotNull
    private TopicName name;

    @NotNull
    private String description;

    @Valid
    @NotNull
    private OwnerId owner;

    private boolean jsonToAvroDryRunEnabled = false;

    @NotNull
    private Ack ack;

    @NotNull
    private ContentType contentType;

    @Min(MIN_MESSAGE_SIZE)
    @Max(MAX_MESSAGE_SIZE)
    private int maxMessageSize;

    public static final int MIN_MESSAGE_SIZE = 1024;
    public static final int MAX_MESSAGE_SIZE = 2 * 1024 * 1024;
    private static final int DEFAULT_MAX_MESSAGE_SIZE = 50 * 1024;

    public enum Ack {
        NONE, LEADER, ALL
    }

    @Valid
    @NotNull
    private RetentionTime retentionTime = RetentionTime.of(1);

    private boolean trackingEnabled = false;

    private boolean migratedFromJsonType = false;

    private boolean schemaVersionAwareSerializationEnabled = false;

    private PublishingAuth publishingAuth;

    public Topic(TopicName name, String description, OwnerId owner, RetentionTime retentionTime,
                 boolean migratedFromJsonType, Ack ack, boolean trackingEnabled, ContentType contentType,
                 boolean jsonToAvroDryRunEnabled, boolean schemaVersionAwareSerializationEnabled,
                 int maxMessageSize, PublishingAuth publishingAuth) {
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.retentionTime = retentionTime;
        this.ack = (ack == null ? Ack.LEADER : ack);
        this.trackingEnabled = trackingEnabled;
        this.migratedFromJsonType = migratedFromJsonType;
        this.contentType = contentType;
        this.jsonToAvroDryRunEnabled = jsonToAvroDryRunEnabled;
        this.schemaVersionAwareSerializationEnabled = schemaVersionAwareSerializationEnabled;
        this.maxMessageSize = maxMessageSize;
        this.publishingAuth = publishingAuth;
    }

    @JsonCreator
    public Topic(
            @JsonProperty("name") String qualifiedName,
            @JsonProperty("description") String description,
            @JsonProperty("owner") OwnerId owner,
            @JsonProperty("retentionTime") RetentionTime retentionTime,
            @JsonProperty("jsonToAvroDryRun") boolean jsonToAvroDryRunEnabled,
            @JsonProperty("ack") Ack ack,
            @JsonProperty("trackingEnabled") boolean trackingEnabled,
            @JsonProperty("migratedFromJsonType") boolean migratedFromJsonType,
            @JsonProperty("schemaVersionAwareSerializationEnabled") boolean schemaVersionAwareSerializationEnabled,
            @JsonProperty("contentType") ContentType contentType,
            @JsonProperty("maxMessageSize") Integer maxMessageSize,
            @JsonProperty("auth") PublishingAuth publishingAuth
            ) {
        this(TopicName.fromQualifiedName(qualifiedName), description, owner, retentionTime, migratedFromJsonType, ack,
                trackingEnabled, contentType, jsonToAvroDryRunEnabled, schemaVersionAwareSerializationEnabled,
                maxMessageSize == null ? DEFAULT_MAX_MESSAGE_SIZE : maxMessageSize,
                publishingAuth == null ? PublishingAuth.disabled() : publishingAuth);
    }

    public RetentionTime getRetentionTime() {
        return retentionTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, owner, retentionTime, migratedFromJsonType, trackingEnabled, ack, contentType,
                jsonToAvroDryRunEnabled, schemaVersionAwareSerializationEnabled, maxMessageSize, publishingAuth);
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
                && Objects.equals(this.owner, other.owner)
                && Objects.equals(this.retentionTime, other.retentionTime)
                && Objects.equals(this.jsonToAvroDryRunEnabled, other.jsonToAvroDryRunEnabled)
                && Objects.equals(this.trackingEnabled, other.trackingEnabled)
                && Objects.equals(this.migratedFromJsonType, other.migratedFromJsonType)
                && Objects.equals(this.schemaVersionAwareSerializationEnabled, other.schemaVersionAwareSerializationEnabled)
                && Objects.equals(this.ack, other.ack)
                && Objects.equals(this.contentType, other.contentType)
                && Objects.equals(this.maxMessageSize, other.maxMessageSize)
                && Objects.equals(this.publishingAuth, other.publishingAuth);
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

    public OwnerId getOwner() {
        return owner;
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

    public boolean isSchemaVersionAwareSerializationEnabled() {
        return schemaVersionAwareSerializationEnabled;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    @JsonProperty("auth")
    public PublishingAuth getPublishingAuth() {
        return publishingAuth;
    }

    @JsonIgnore
    public boolean isAuthEnabled() {
        return publishingAuth.isEnabled();
    }

    @JsonIgnore
    public boolean isUnauthenticatedAccessEnabled() {
        return publishingAuth.isUnauthenticatedAccessEnabled();
    }

    public boolean hasPermission(String publisher) {
        return publishingAuth.hasPermission(publisher);
    }

    @Override
    public String toString() {
        return "Topic(" + getQualifiedName() + ")";
    }
}
