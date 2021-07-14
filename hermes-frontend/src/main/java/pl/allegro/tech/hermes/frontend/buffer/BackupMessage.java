package pl.allegro.tech.hermes.frontend.buffer;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class BackupMessage implements Serializable {

    private final String messageId;
    private final byte[] data;
    private final long timestamp;
    private final String qualifiedTopicName;
    private final String partitionKey;

    public BackupMessage(String messageId, byte[] data, long timestamp, String qualifiedTopicName, String partitionKey) {
        this.messageId = messageId;
        this.data = data;
        this.timestamp = timestamp;
        this.qualifiedTopicName = qualifiedTopicName;
        this.partitionKey = partitionKey;
    }

    public String getMessageId() {
        return messageId;
    }

    public byte[] getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getQualifiedTopicName() {
        return qualifiedTopicName;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof BackupMessage)) return false;
        BackupMessage that = (BackupMessage) o;
        return Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(messageId, that.messageId) &&
                Arrays.equals(data, that.data) &&
                Objects.equals(qualifiedTopicName, that.qualifiedTopicName) &&
                Objects.equals(partitionKey, that.partitionKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, data, timestamp, qualifiedTopicName, partitionKey);
    }
}
