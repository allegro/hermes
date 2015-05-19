package pl.allegro.tech.hermes.consumers.consumer.receiver;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
public class Message {

    private final Optional<String> id;
    private final long offset;

    private final int partition;

    private String topic;
    private Optional<Long> publishingTimestamp;
    private Optional<Long> readingTimestamp;

    private final byte[] data;

    public Message(Optional<String> id, long offset, int partition, String topic, byte[] content, Optional<Long> publishingTimestamp,
                   Optional<Long> readingTimestamp) {
        this.id = id;
        this.offset = offset;
        this.partition = partition;
        this.data = content;
        this.topic = topic;
        this.publishingTimestamp = publishingTimestamp;
        this.readingTimestamp = readingTimestamp;
    }

    public Optional<Long> getPublishingTimestamp() {
        return publishingTimestamp;
    }

    public Optional<Long> getReadingTimestamp() {
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

        return readingTimestamp.isPresent() && currentTimestamp > readingTimestamp.get() + TimeUnit.SECONDS.toMillis(ttlSeconds);
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

    public boolean isEmpty() {
        return ArrayUtils.isEmpty(data);
    }

    public Optional<String> getId() {
        return id;
    }
}
