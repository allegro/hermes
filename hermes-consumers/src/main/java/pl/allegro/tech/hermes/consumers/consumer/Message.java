package pl.allegro.tech.hermes.consumers.consumer;

import com.google.common.collect.ImmutableMap;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

import java.util.Map;
import java.util.Objects;

public class Message {

    private String id;
    private PartitionOffset partitionOffset;

    private String topic;
    private ContentType contentType;

    private long publishingTimestamp;
    private long readingTimestamp;
    private byte[] data;

    private int retryCounter = 0;

    private Map<String, String> externalMetadata;

    private Message() {}

    public Message(String id,
                   String topic,
                   byte[] content,
                   ContentType contentType,
                   long publishingTimestamp,
                   long readingTimestamp,
                   PartitionOffset partitionOffset,
                   Map<String, String> externalMetadata) {
        this.id = id;
        this.data = content;
        this.topic = topic;
        this.contentType = contentType;
        this.publishingTimestamp = publishingTimestamp;
        this.readingTimestamp = readingTimestamp;
        this.partitionOffset = partitionOffset;
        this.externalMetadata = externalMetadata;
    }

    public String getId() {
        return id;
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

    public byte[] getData() {
        return data;
    }

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

    public void incrementRetryCounter() {
        this.retryCounter++;
    }

    public int getRetryCounter() {
        return retryCounter;
    }

    public Map<String, String> getExternalMetadata() {
        return ImmutableMap.copyOf(externalMetadata);
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
            this.message.publishingTimestamp = message.getPublishingTimestamp();
            this.message.readingTimestamp = message.getReadingTimestamp();
            this.message.partitionOffset = message.partitionOffset;
            this.message.externalMetadata = message.getExternalMetadata();

            return this;
        }

        public Builder withData(byte [] data) {
            this.message.data = data;
            return this;
        }

        public Builder withExternalMetadata(Map<String, String> externalMetadata) {
            this.message.externalMetadata = externalMetadata;
            return this;
        }

        public Message build() {
            return message;
        }

        public Builder withContentType(ContentType contentType) {
            this.message.contentType = contentType;

            return this;
        }
    }

}
