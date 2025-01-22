package pl.allegro.tech.hermes.common.message.wrapper;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageMetadata {

  private final long timestamp;
  private final String id;
  private final Map<String, String> externalMetadata;

  @JsonCreator
  public MessageMetadata(
      @JsonProperty("timestamp") long timestamp,
      @JsonProperty("id") String id,
      @JsonProperty("externalMetadata") Map<String, String> externalMetadata) {
    this.id = id;
    this.timestamp = timestamp;
    this.externalMetadata =
        ofNullable(externalMetadata).orElseGet(ImmutableMap::<String, String>of);
  }

  public MessageMetadata(long timestamp, Map<String, String> externalMetadata) {
    this(timestamp, "", externalMetadata);
  }

  public String getId() {
    return id;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Map<String, String> getExternalMetadata() {
    return ImmutableMap.copyOf(externalMetadata);
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
