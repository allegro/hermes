package pl.allegro.tech.hermes.frontend.buffer;

import com.google.common.collect.ImmutableMap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class BackupMessage implements Serializable {

    private final String messageId;
    private final byte[] data;
    private final long timestamp;
    private final String qualifiedTopicName;
    private final String partitionKey;
    private final Map<String, String> extraRequestHeaders;

    public BackupMessage(String messageId, byte[] data, long timestamp, String qualifiedTopicName, String partitionKey, Map<String, String> extraRequestHeaders) {
        this.messageId = messageId;
        this.data = data;
        this.timestamp = timestamp;
        this.qualifiedTopicName = qualifiedTopicName;
        this.partitionKey = partitionKey;
        this.extraRequestHeaders = ImmutableMap.copyOf(extraRequestHeaders);
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

    public Map<String, String> getExtraRequestHeaders() {
        return extraRequestHeaders;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BackupMessage)) return false;
        BackupMessage that = (BackupMessage) o;
        return Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(messageId, that.messageId) &&
                Arrays.equals(data, that.data) &&
                Objects.equals(qualifiedTopicName, that.qualifiedTopicName) &&
                Objects.equals(partitionKey, that.partitionKey) &&
                Objects.equals(extraRequestHeaders, that.extraRequestHeaders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, data, timestamp, qualifiedTopicName, partitionKey, extraRequestHeaders);
    }
}
