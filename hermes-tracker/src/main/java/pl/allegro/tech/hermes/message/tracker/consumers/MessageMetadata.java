package pl.allegro.tech.hermes.message.tracker.consumers;

import java.util.Optional;

public class MessageMetadata {

    private final String id;
    private final long offset;
    private final int partition;
    private final String topic;
    private final String subscription;
    private final Optional<Long> publishingTimestamp;
    private final Optional<Long> readingTimestamp;

    public MessageMetadata(String id, long offset, int partition, String topic, String subscription,
                           Optional<Long> publishingTimestamp, Optional<Long> readingTimestamp) {
        this.id = id;
        this.offset = offset;
        this.partition = partition;
        this.topic = topic;
        this.subscription = subscription;
        this.publishingTimestamp = publishingTimestamp;
        this.readingTimestamp = readingTimestamp;
    }

    public String getId() {
        return id;
    }

    public long getOffset() {
        return offset;
    }

    public int getPartition() {
        return partition;
    }

    public String getTopic() {
        return topic;
    }

    public String getSubscription() {
        return subscription;
    }

    public Optional<Long> getPublishingTimestamp() {
        return publishingTimestamp;
    }

    public Optional<Long> getReadingTimestamp() {
        return readingTimestamp;
    }
}
