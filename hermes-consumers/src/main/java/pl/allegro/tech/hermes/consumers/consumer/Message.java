package pl.allegro.tech.hermes.consumers.consumer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.avro.Schema;
import org.apache.commons.lang3.ArrayUtils;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Header;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.domain.filtering.FilterableMessage;
import pl.allegro.tech.hermes.schema.CompiledSchema;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public class Message implements FilterableMessage {

    private String id;
    private PartitionOffset partitionOffset;

    private String topic;
    private String subscription;
    private boolean hasSubscriptionIdentityHeaders;
    private ContentType contentType;
    private Optional<CompiledSchema<Schema>> schema;

    private long publishingTimestamp;
    private long readingTimestamp;
    private byte[] data;

    private int retryCounter = 0;
    private long partitionAssignmentTerm = -1;
    private Map<String, String> externalMetadata = Collections.emptyMap();

    private List<Header> additionalHeaders = Collections.emptyList();

    private Set<String> succeededUris = Sets.newHashSet();

    private long currentMessageBackoff = -1;

    private Message() {}

    public Message(String id,
                   String topic,
                   byte[] content,
                   ContentType contentType,
                   Optional<CompiledSchema<Schema>> schema,
                   long publishingTimestamp,
                   long readingTimestamp,
                   PartitionOffset partitionOffset,
                   long partitionAssignmentTerm,
                   Map<String, String> externalMetadata,
                   List<Header> additionalHeaders,
                   String subscription,
                   boolean hasSubscriptionIdentityHeaders) {
        this.id = id;
        this.data = content;
        this.topic = topic;
        this.contentType = contentType;
        this.schema = schema;
        this.publishingTimestamp = publishingTimestamp;
        this.readingTimestamp = readingTimestamp;
        this.partitionOffset = partitionOffset;
        this.partitionAssignmentTerm = partitionAssignmentTerm;
        this.externalMetadata = ImmutableMap.copyOf(externalMetadata);
        this.additionalHeaders = ImmutableList.copyOf(additionalHeaders);
        this.subscription = subscription;
        this.hasSubscriptionIdentityHeaders = hasSubscriptionIdentityHeaders;
    }

    public long getPublishingTimestamp() {
        return publishingTimestamp;
    }

    public long getReadingTimestamp() {
        return readingTimestamp;
    }

    public long getOffset() {
        return partitionOffset.getOffset();
    }

    public long getPartitionAssignmentTerm() {
        return partitionAssignmentTerm;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public ContentType getContentType() {
        return contentType;
    }

    public int getPartition() {
        return partitionOffset.getPartition();
    }

    public String getTopic() {
        return topic;
    }

    public boolean isTtlExceeded(long ttlMillis) {
        long currentTimestamp = System.currentTimeMillis();
        return currentTimestamp > readingTimestamp + ttlMillis;
    }

    public void incrementRetryCounter(Collection<URI> succeededUris) {
        this.retryCounter++;
        this.succeededUris.addAll(succeededUris.stream().map(URI::toString).collect(toList()));
    }

    public int getRetryCounter() {
        return retryCounter;
    }

    @Override
    public Optional<CompiledSchema<Schema>> getSchema() {
        return schema;
    }

    public String getId() {
        return id;
    }

    @Override
    public Map<String, String> getExternalMetadata() {
        return Collections.unmodifiableMap(externalMetadata);
    }

    public List<Header> getAdditionalHeaders() {
        return Collections.unmodifiableList(additionalHeaders);
    }

    public long updateAndGetCurrentMessageBackoff(SubscriptionPolicy subscriptionPolicy) {
        if (currentMessageBackoff == -1) {
            currentMessageBackoff = subscriptionPolicy.getMessageBackoff();
        } else {
            currentMessageBackoff = Math.min(subscriptionPolicy.getBackoffMaxIntervalMillis(),
                    (long) (currentMessageBackoff * subscriptionPolicy.getBackoffMultiplier()));
        }
        return currentMessageBackoff;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Message other = (Message) obj;
        return Objects.equals(this.id, other.id);
    }

    public static Builder message() {
        return new Builder();
    }

    public KafkaTopicName getKafkaTopic() {
        return partitionOffset.getTopic();
    }

    public PartitionOffset getPartitionOffset() {
        return partitionOffset;
    }

    public boolean hasNotBeenSentTo(String uri) {
        return !succeededUris.contains(uri);
    }

    public long getSize() {
        return ArrayUtils.getLength(data);
    }

    public boolean hasSubscriptionIdentityHeaders() {
        return hasSubscriptionIdentityHeaders;
    }

    public String getSubscription() {
        return subscription;
    }

    public static class Builder {

        private final Message message;

        public Builder() {
            message = new Message();
        }

        public Builder fromMessage(Message message) {
            this.message.id = message.getId();
            this.message.data = message.getData();
            this.message.contentType = message.getContentType();
            this.message.topic = message.getTopic();
            this.message.subscription = message.getSubscription();
            this.message.hasSubscriptionIdentityHeaders = message.hasSubscriptionIdentityHeaders();
            this.message.publishingTimestamp = message.getPublishingTimestamp();
            this.message.readingTimestamp = message.getReadingTimestamp();
            this.message.partitionOffset = message.partitionOffset;
            this.message.partitionAssignmentTerm = message.partitionAssignmentTerm;
            this.message.externalMetadata = message.getExternalMetadata();
            this.message.additionalHeaders = message.getAdditionalHeaders();
            this.message.schema = message.getSchema();

            return this;
        }

        public Builder withData(byte [] data) {
            this.message.data = data;
            return this;
        }

        public Builder withSchema(CompiledSchema<Schema> schema) {
            this.message.schema = Optional.of(schema);
            return this;
        }

        public Builder withExternalMetadata(Map<String, String> externalMetadata) {
            this.message.externalMetadata = ImmutableMap.copyOf(externalMetadata);
            return this;
        }

        public Builder withAdditionalHeaders(List<Header> additionalHeaders) {
            this.message.additionalHeaders = ImmutableList.copyOf(additionalHeaders);
            return this;
        }

        public Builder withContentType(ContentType contentType) {
            this.message.contentType = contentType;

            return this;
        }

        public Builder withNoSchema() {
            this.message.schema = Optional.empty();
            return this;
        }

        public Message build() {
            return message;
        }
    }
}
