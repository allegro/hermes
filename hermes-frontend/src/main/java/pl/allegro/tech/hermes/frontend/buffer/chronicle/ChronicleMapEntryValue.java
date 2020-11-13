package pl.allegro.tech.hermes.frontend.buffer.chronicle;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class ChronicleMapEntryValue implements Serializable {
    static final long serialVersionUID = -2149667159974528954L;

    private final byte[] data;
    private final long timestamp;
    private final String qualifiedTopicName;
    private final String partitionKey;

    public ChronicleMapEntryValue(byte[] data, long timestamp, String qualifiedTopicName, String partitionKey) {
        this.data = data;
        this.timestamp = timestamp;
        this.qualifiedTopicName = qualifiedTopicName;
        this.partitionKey = partitionKey;
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
        if (o == null || !(o instanceof ChronicleMapEntryValue)) return false;
        ChronicleMapEntryValue that = (ChronicleMapEntryValue) o;
        return Objects.equals(timestamp, that.timestamp) &&
                Arrays.equals(data, that.data) &&
                Objects.equals(qualifiedTopicName, that.qualifiedTopicName) &&
                Objects.equals(partitionKey, that.partitionKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, timestamp, qualifiedTopicName, partitionKey);
    }
}
