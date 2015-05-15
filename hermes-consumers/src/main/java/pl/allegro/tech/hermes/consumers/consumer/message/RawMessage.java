package pl.allegro.tech.hermes.consumers.consumer.message;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Optional;

public class RawMessage {

    private Optional<String> messageId;
    private int partition;
    private Long offset;
    private String topic;
    private Optional<Long> timestamp;
    private byte[] data;

    @SuppressWarnings("all")
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public RawMessage(Optional<String> messageId, long offset, int partition, String topic, byte[] content, Optional<Long> timestamp) {
        this.messageId = messageId;
        this.offset = offset;
        this.partition = partition;
        this.data = content;
        this.topic = topic;
        this.timestamp = timestamp;
    }

    public RawMessage() {
    }

    public String getTopic() {
        return topic;
    }

    public long getOffset() {
        return offset;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public byte[] getData() {
        return data;
    }

    public MessageStatus getStatus() {
        return data == null ? MessageStatus.EMPTY : MessageStatus.OK;
    }

    public int getPartition() {
        return partition;
    }

    public Optional<Long> getTimestamp() {
        return timestamp;
    }

    public Optional<String> getId() {
        return messageId;
    }
}
