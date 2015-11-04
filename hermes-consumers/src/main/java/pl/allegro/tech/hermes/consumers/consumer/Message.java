package pl.allegro.tech.hermes.consumers.consumer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
public class Message {

    private String id;
    private PartitionOffset partitionOffset;

    private String topic;
    private Topic.ContentType contentType;

    private long publishingTimestamp;
    private long readingTimestamp;
    private byte[] data;
    private String traceId;

    private Message() {}

    public Message(String id, String topic, String traceId, byte[] content, Topic.ContentType contentType, long publishingTimestamp,
                   long readingTimestamp, PartitionOffset partitionOffset) {
        this.id = id;
        this.traceId = traceId;
        this.data = content;
        this.topic = topic;
        this.contentType = contentType;
        this.publishingTimestamp = publishingTimestamp;
        this.readingTimestamp = readingTimestamp;
        this.partitionOffset = partitionOffset;
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

    public String getTraceId() {
        return traceId;
    }

    public byte[] getData() {
        return data;
    }

    public Topic.ContentType getContentType() {
        return contentType;
    }

    public int getPartition() {
        return partitionOffset.getPartition();
    }

    public String getTopic() {
        return topic;
    }

    public boolean isTtlExceeded(int ttlSeconds) {

        long currentTimestamp = System.currentTimeMillis();

        return currentTimestamp > readingTimestamp + TimeUnit.SECONDS.toMillis(ttlSeconds);
    }

    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, data, contentType, publishingTimestamp, readingTimestamp, partitionOffset);
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
        return Objects.equals(this.topic, other.topic)
                && Objects.equals(this.publishingTimestamp, other.publishingTimestamp)
                && Objects.equals(this.readingTimestamp, other.readingTimestamp)
                && Arrays.equals(this.data, other.data)
                && Objects.equals(this.contentType, other.contentType)
                && Objects.equals(this.partitionOffset, other.partitionOffset);
    }

    public static Builder message() {
        return new Builder();
    }

    public KafkaTopicName getKafkaTopic() {
        return partitionOffset.getTopic();
    }

    public static class Builder {
        private final Message message;

        public Builder() {
            message = new Message();
        }

        public Builder fromMessage(Message message) {
            this.message.id = message.getId();
            this.message.traceId = message.getTraceId();
            this.message.data = message.getData();
            this.message.contentType = message.getContentType();
            this.message.topic = message.getTopic();
            this.message.publishingTimestamp = message.getPublishingTimestamp();
            this.message.readingTimestamp = message.getReadingTimestamp();
            this.message.partitionOffset = message.partitionOffset;

            return this;
        }

        public Builder withData(byte [] data) {
            this.message.data = data;
            return this;
        }

        public Message build() {
            return message;
        }
    }

}
