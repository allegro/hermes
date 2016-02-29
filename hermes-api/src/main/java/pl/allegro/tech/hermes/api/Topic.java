package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Objects;

public class Topic {

    @Valid
    @NotNull
    private TopicName name;

    @NotNull
    private String description;

    private String messageSchema;

    private boolean validationEnabled = false;

    private boolean validationDryRunEnabled = false;

    private boolean jsonToAvroDryRunEnabled = false;

    @NotNull
    private Ack ack;

    @NotNull
    private ContentType contentType;

    public enum Ack {
        NONE, LEADER, ALL
    }

    @Valid
    @NotNull
    private RetentionTime retentionTime = RetentionTime.of(1);

    private boolean trackingEnabled = false;

    private boolean migratedFromJsonType = false;

    private boolean schemaVersionAwareSerialization = false;

    public Topic(TopicName name, String description, RetentionTime retentionTime, String messageSchema,
                 boolean validationEnabled, boolean validationDryRunEnabled, boolean migratedFromJsonType,
                 Ack ack, boolean trackingEnabled, ContentType contentType, boolean jsonToAvroDryRunEnabled,
                 boolean schemaVersionAwareSerialization) {
        this.name = name;
        this.description = description;
        this.retentionTime = retentionTime;
        this.messageSchema = messageSchema;
        this.validationEnabled = validationEnabled;
        this.validationDryRunEnabled = validationDryRunEnabled;
        this.ack = (ack == null ? Ack.LEADER : ack);
        this.trackingEnabled = trackingEnabled;
        this.migratedFromJsonType = migratedFromJsonType;
        this.contentType = contentType;
        this.jsonToAvroDryRunEnabled = jsonToAvroDryRunEnabled;
        this.schemaVersionAwareSerialization = schemaVersionAwareSerialization;
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
            @JsonProperty("schemaVersionAwareSerialization") boolean schemaVersionAwareSerialization,
            @JsonProperty("contentType") ContentType contentType) {
        this(TopicName.fromQualifiedName(qualifiedName), description, retentionTime, messageSchema, validationEnabled,
                validationDryRunEnabled, migratedFromJsonType, ack, trackingEnabled, contentType, jsonToAvroDryRunEnabled,
                schemaVersionAwareSerialization);
    }

    public RetentionTime getRetentionTime() {
        return retentionTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, retentionTime, messageSchema, validationEnabled, validationDryRunEnabled,
                migratedFromJsonType, trackingEnabled, ack, contentType, jsonToAvroDryRunEnabled, schemaVersionAwareSerialization);
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
                && Objects.equals(this.schemaVersionAwareSerialization, other.schemaVersionAwareSerialization)
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

    public boolean isSchemaVersionAwareSerialization() {
        return schemaVersionAwareSerialization;
    }
}
