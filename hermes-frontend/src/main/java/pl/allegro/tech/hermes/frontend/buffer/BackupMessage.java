package pl.allegro.tech.hermes.frontend.buffer;

import pl.allegro.tech.hermes.domain.topic.schema.SchemaVersion;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class BackupMessage {

    private final String messageId;
    private final byte[] data;
    private final long timestamp;
    private final String qualifiedTopicName;
    private final Optional<SchemaVersion> schemaVersionOptional;

    public BackupMessage(String messageId, byte[] data, long timestamp, String qualifiedTopicName, Optional<SchemaVersion> schemaVersionOptional) {
        this.messageId = messageId;
        this.data = data;
        this.timestamp = timestamp;
        this.qualifiedTopicName = qualifiedTopicName;
        this.schemaVersionOptional = schemaVersionOptional;
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

    public Optional<SchemaVersion> getSchemaVersionOptional() {
        return schemaVersionOptional;
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
                Objects.equals(schemaVersionOptional, that.schemaVersionOptional);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, data, timestamp, qualifiedTopicName, schemaVersionOptional);
    }
}
