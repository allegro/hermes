package pl.allegro.tech.hermes.test.helper.builder;

import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.OfflineRetentionTime;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PublishingAuth;
import pl.allegro.tech.hermes.api.RetentionTime;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicDataOfflineStorage;
import pl.allegro.tech.hermes.api.TopicName;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TopicBuilder {

    private final TopicName name;

    private String description = "description";

    private OwnerId owner = new OwnerId("Plaintext", "some team");

    private boolean jsonToAvroDryRunEnabled = false;

    private Topic.Ack ack = Topic.Ack.LEADER;

    private ContentType contentType = ContentType.JSON;

    private RetentionTime retentionTime = RetentionTime.of(1);

    private boolean trackingEnabled = false;

    private boolean migratedFromJsonType = false;

    private boolean schemaVersionAwareSerialization = false;

    private int maxMessageSize = 1024 * 1024;

    private List<String> publishers = new ArrayList<>();

    private boolean authEnabled = false;

    private boolean unauthenticatedAccessEnabled = true;

    private boolean subscribingRestricted = false;

    private TopicDataOfflineStorage offlineStorage = TopicDataOfflineStorage.defaultOfflineStorage();

    private TopicBuilder(TopicName topicName) {
        this.name = topicName;
    }

    public static TopicBuilder randomTopic(String group, String topicNamePrefix) {
        return topic(group, topicNamePrefix + "-" + UUID.randomUUID());
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
                name, description, owner, retentionTime, migratedFromJsonType, ack, trackingEnabled, contentType,
                jsonToAvroDryRunEnabled, schemaVersionAwareSerialization, maxMessageSize,
                new PublishingAuth(publishers, authEnabled, unauthenticatedAccessEnabled), subscribingRestricted,
                offlineStorage, null, null
        );
    }

    public TopicBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public TopicBuilder withOwner(OwnerId owner) {
        this.owner = owner;
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

    public TopicBuilder withSubscribingRestricted() {
        this.subscribingRestricted = true;
        return this;
    }

    public TopicBuilder withMaxMessageSize(int size) {
        this.maxMessageSize = size;
        return this;
    }

    public TopicBuilder withPublisher(String serviceName) {
        this.publishers.add(serviceName);
        return this;
    }

    public TopicBuilder withAuthEnabled() {
        this.authEnabled = true;
        return this;
    }

    public TopicBuilder withUnauthenticatedAccessDisabled() {
        this.unauthenticatedAccessEnabled = false;
        return this;
    }

    public TopicBuilder withOfflineStorage(int days) {
        this.offlineStorage = new TopicDataOfflineStorage(true, OfflineRetentionTime.of(days));
        return this;
    }
}
