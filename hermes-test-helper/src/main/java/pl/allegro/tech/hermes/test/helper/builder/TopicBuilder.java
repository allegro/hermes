package pl.allegro.tech.hermes.test.helper.builder;

import pl.allegro.tech.hermes.api.*;

public class TopicBuilder {

    private final TopicName name;

    private String description = "description";

    private MaintainerDescriptor maintainer = new MaintainerDescriptor("Simple", "some team");

    private boolean jsonToAvroDryRunEnabled = false;

    private Topic.Ack ack = Topic.Ack.LEADER;

    private ContentType contentType = ContentType.JSON;

    private RetentionTime retentionTime = RetentionTime.of(1);

    private boolean trackingEnabled = false;

    private boolean migratedFromJsonType = false;

    private boolean schemaVersionAwareSerialization = false;

    private int maxMessageSize = 1024 * 1024;

    private TopicBuilder(TopicName topicName) {
        this.name = topicName;
    }

    public static TopicBuilder topic(TopicName topicName) {
        return new TopicBuilder(topicName);
    }

    public static TopicBuilder topic(String groupName, String topicName) {
        return new TopicBuilder(new TopicName(groupName, topicName));
    }

    public static TopicBuilder topic(String qualifiedName) {
        return new TopicBuilder(TopicName.fromQualifiedName(qualifiedName));
    }

    public Topic build() {
        return new Topic(
                name, description, maintainer, retentionTime, migratedFromJsonType, ack, trackingEnabled, contentType,
                jsonToAvroDryRunEnabled, schemaVersionAwareSerialization, maxMessageSize
        );
    }

    public TopicBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public TopicBuilder withMaintainer(MaintainerDescriptor maintainer) {
        this.maintainer = maintainer;
        return this;
    }

    public TopicBuilder withRetentionTime(RetentionTime retentionTime) {
        this.retentionTime = retentionTime;
        return this;
    }

    public TopicBuilder withRetentionTime(int retentionTime) {
        this.retentionTime = new RetentionTime(retentionTime);
        return this;
    }

    public TopicBuilder withJsonToAvroDryRun(boolean enabled) {
        this.jsonToAvroDryRunEnabled = enabled;
        return this;
    }

    public TopicBuilder withAck(Topic.Ack ack) {
        this.ack = ack;
        return this;
    }

    public TopicBuilder withTrackingEnabled(boolean enabled) {
        this.trackingEnabled = enabled;
        return this;
    }

    public TopicBuilder withContentType(ContentType contentType) {
        this.contentType = contentType;
        return this;
    }

    public TopicBuilder migratedFromJsonType() {
        this.migratedFromJsonType = true;
        return this;
    }

    public TopicBuilder withSchemaVersionAwareSerialization() {
        this.schemaVersionAwareSerialization = true;
        return this;
    }

    public TopicBuilder withMaxMessageSize(int size) {
        this.maxMessageSize = size;
        return this;
    }
}
