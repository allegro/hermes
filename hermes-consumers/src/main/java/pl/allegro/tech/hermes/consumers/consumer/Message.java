package pl.allegro.tech.hermes.consumers.consumer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
public class Message {

    private String id;
    private long offset;

    private int partition;

    private String topic;
    private long publishingTimestamp;
    private long readingTimestamp;

    private byte[] data;

    private Message() {}

    public Message(String id, long offset, int partition, String topic, byte[] content, long publishingTimestamp, long readingTimestamp) {
        this.id = id;
        this.offset = offset;
        this.partition = partition;
        this.data = content;
        this.topic = topic;
        this.publishingTimestamp = publishingTimestamp;
        this.readingTimestamp = readingTimestamp;
    }

    public long getPublishingTimestamp() {
        return publishingTimestamp;
    }

    public long getReadingTimestamp() {
        return readingTimestamp;
    }

    public long getOffset() {
        return offset;
    }

    public byte[] getData() {
        return data;
    }

    public int getPartition() {
        return partition;
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
        return Objects.hash(offset, partition, topic, data, publishingTimestamp, readingTimestamp);
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
        return Objects.equals(this.offset, other.offset)
                && Objects.equals(this.partition, other.partition)
                && Objects.equals(this.topic, other.topic)
                && Objects.equals(this.publishingTimestamp, other.publishingTimestamp)
                && Objects.equals(this.readingTimestamp, other.readingTimestamp)
                && Arrays.equals(this.data, other.data);
    }

    public static Builder message() {
        return new Builder();
    }

    public static class Builder {
        private final Message message;

        public Builder() {
            message = new Message();
        }

        public Builder fromMessage(Message message) {
            this.message.id = message.getId();
            this.message.offset = message.getOffset();
            this.message.partition = message.getPartition();
            this.message.data = message.getData();
            this.message.topic = message.getTopic();
            this.message.publishingTimestamp = message.getPublishingTimestamp();
            this.message.readingTimestamp = message.getReadingTimestamp();

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
