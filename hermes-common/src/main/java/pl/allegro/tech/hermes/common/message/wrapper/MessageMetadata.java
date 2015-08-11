package pl.allegro.tech.hermes.common.message.wrapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class MessageMetadata {

    private final Long timestamp;
    private final String id;

    @JsonCreator
    public MessageMetadata(@JsonProperty("timestamp") Long timestamp, @JsonProperty("id") String id) {
        this.id = id;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final MessageMetadata other = (MessageMetadata) obj;
        return Objects.equals(this.id, other.id) && Objects.equals(this.timestamp, other.timestamp);
    }
}
