package pl.allegro.tech.hermes.tracker.consumers;

public class MessageMetadata {

    private final String id;
    private final long offset;
    private final int partition;
    private final String topic;
    private final String subscription;
    private final long publishingTimestamp;
    private final long readingTimestamp;

    public MessageMetadata(String id, long offset, int partition, String topic, String subscription,
                           long publishingTimestamp, long readingTimestamp) {
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

    public long getPublishingTimestamp() {
        return publishingTimestamp;
    }

    public long getReadingTimestamp() {
        return readingTimestamp;
    }
}
