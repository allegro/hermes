package pl.allegro.tech.hermes.test.helper.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.OfflineRetentionTime;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PublishingAuth;
import pl.allegro.tech.hermes.api.PublishingChaosPolicy;
import pl.allegro.tech.hermes.api.RetentionTime;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicDataOfflineStorage;
import pl.allegro.tech.hermes.api.TopicLabel;
import pl.allegro.tech.hermes.api.TopicName;

public class TopicBuilder {

  private static final AtomicLong sequence = new AtomicLong();

  private final TopicName name;

  private String description = "description";

  private OwnerId owner = new OwnerId("Plaintext", "some team");

  private boolean jsonToAvroDryRunEnabled = false;

  private Topic.Ack ack = Topic.Ack.LEADER;

  private boolean fallbackToRemoteDatacenterEnabled = false;

  private PublishingChaosPolicy chaos = PublishingChaosPolicy.disabled();

  private ContentType contentType = ContentType.JSON;

  private RetentionTime retentionTime = RetentionTime.of(1, TimeUnit.DAYS);

  private boolean trackingEnabled = false;

  private boolean migratedFromJsonType = false;

  private boolean schemaIdAwareSerialization = false;

  private int maxMessageSize = 1024 * 1024;

  private final List<String> publishers = new ArrayList<>();

  private boolean authEnabled = false;

  private boolean unauthenticatedAccessEnabled = true;

  private boolean subscribingRestricted = false;

  private TopicDataOfflineStorage offlineStorage = TopicDataOfflineStorage.defaultOfflineStorage();

  private Set<TopicLabel> labels = Collections.emptySet();

  private TopicBuilder(TopicName topicName) {
    this.name = topicName;
  }

  public static TopicBuilder topicWithRandomName() {
    return topicWithRandomNameEndedWith("");
  }

  public static TopicBuilder topicWithRandomNameContaining(String string) {
    return topic(
        TopicBuilder.class.getSimpleName() + "Group" + sequence.incrementAndGet(),
        TopicBuilder.class.getSimpleName() + "Topic" + string + sequence.incrementAndGet());
  }

  public static TopicBuilder topicWithRandomNameEndedWith(String suffix) {
    return topic(
        TopicBuilder.class.getSimpleName() + "Group" + sequence.incrementAndGet(),
        TopicBuilder.class.getSimpleName() + "Topic" + sequence.incrementAndGet() + suffix);
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
        name,
        description,
        owner,
        retentionTime,
        migratedFromJsonType,
        ack,
        fallbackToRemoteDatacenterEnabled,
        chaos,
        trackingEnabled,
        contentType,
        jsonToAvroDryRunEnabled,
        schemaIdAwareSerialization,
        maxMessageSize,
        new PublishingAuth(publishers, authEnabled, unauthenticatedAccessEnabled),
        subscribingRestricted,
        offlineStorage,
        labels,
        null,
        null);
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

  public TopicBuilder withRetentionTime(int retentionTime, TimeUnit unit) {
    this.retentionTime = new RetentionTime(retentionTime, unit);
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

  public TopicBuilder withFallbackToRemoteDatacenterEnabled() {
    this.fallbackToRemoteDatacenterEnabled = true;
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

  public TopicBuilder withSchemaIdAwareSerialization() {
    this.schemaIdAwareSerialization = true;
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

  public TopicBuilder withLabels(Set<TopicLabel> labels) {
    this.labels = labels;
    return this;
  }

  public TopicBuilder withPublishingChaosPolicy(PublishingChaosPolicy chaos) {
    this.chaos = chaos;
    return this;
  }
}
