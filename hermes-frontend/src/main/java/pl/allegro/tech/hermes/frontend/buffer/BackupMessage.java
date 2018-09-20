package pl.allegro.tech.hermes.frontend.buffer;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class BackupMessage implements Serializable {

    private final String messageId;
    private final byte[] data;
    private final long timestamp;
    private final String qualifiedTopicName;

    public BackupMessage(String messageId, byte[] data, long timestamp, String qualifiedTopicName) {
        this.messageId = messageId;
        this.data = data;
        this.timestamp = timestamp;
        this.qualifiedTopicName = qualifiedTopicName;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof BackupMessage)) return false;
        BackupMessage that = (BackupMessage) o;
        return Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(messageId, that.messageId) &&
                Arrays.equals(data, that.data) &&
                Objects.equals(qualifiedTopicName, that.qualifiedTopicName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, data, timestamp, qualifiedTopicName);
    }
}
