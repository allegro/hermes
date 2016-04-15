package pl.allegro.tech.hermes.frontend.buffer.chronicle;

import pl.allegro.tech.hermes.domain.topic.schema.SchemaVersion;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class ChronicleMapEntryValue implements Serializable {

    private final byte[] data;
    private final long timestamp;
    private final String qualifiedTopicName;
    private final SchemaVersion schemaVersion;

    public ChronicleMapEntryValue(byte[] data, long timestamp, String qualifiedTopicName, SchemaVersion schemaVersion) {
        this.data = data;
        this.timestamp = timestamp;
        this.qualifiedTopicName = qualifiedTopicName;
        this.schemaVersion = schemaVersion;
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

    public SchemaVersion getSchemaVersion() {
        return schemaVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ChronicleMapEntryValue)) return false;
        ChronicleMapEntryValue that = (ChronicleMapEntryValue) o;
        return Objects.equals(timestamp, that.timestamp) &&
                Arrays.equals(data, that.data) &&
                Objects.equals(qualifiedTopicName, that.qualifiedTopicName) &&
                Objects.equals(schemaVersion, that.schemaVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, timestamp, qualifiedTopicName, schemaVersion);
    }
}
