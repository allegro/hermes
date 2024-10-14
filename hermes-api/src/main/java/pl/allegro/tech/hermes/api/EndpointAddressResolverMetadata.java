package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.collect.ImmutableMap;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@JsonSerialize(
    using = EndpointAddressResolverMetadata.EndpointAddressResolverMetadataSerializer.class)
public class EndpointAddressResolverMetadata {

  private static final EndpointAddressResolverMetadata EMPTY_INSTANCE =
      new EndpointAddressResolverMetadata(Collections.emptyMap());

  @NotNull private Map<String, Object> entries;

  @JsonCreator
  public EndpointAddressResolverMetadata(Map<String, Object> entries) {
    this.entries = ImmutableMap.copyOf(entries);
  }

  public static EndpointAddressResolverMetadata empty() {
    return EMPTY_INSTANCE;
  }

  public static Builder endpointAddressResolverMetadata() {
    return new Builder();
  }

  public Optional<Object> get(String key) {
    return Optional.ofNullable(entries.get(key));
  }

  @SuppressWarnings("unchecked")
  public <T> T getOrDefault(String key, T defaultValue) {
    return (T) entries.getOrDefault(key, defaultValue);
  }

  public Map<String, Object> getEntries() {
    return entries;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EndpointAddressResolverMetadata that = (EndpointAddressResolverMetadata) o;
    return Objects.equals(entries, that.entries);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entries);
  }

  public static class EndpointAddressResolverMetadataSerializer
      extends StdSerializer<EndpointAddressResolverMetadata> {

    protected EndpointAddressResolverMetadataSerializer() {
      super(EndpointAddressResolverMetadata.class);
    }

    @Override
    public void serialize(
        EndpointAddressResolverMetadata metadata, JsonGenerator jgen, SerializerProvider provider)
        throws IOException {
      jgen.writeObject(metadata.entries);
    }
  }

  public static class Builder {

    private Map<String, Object> entries = new HashMap<>();

    public Builder withEntry(String key, Object value) {
      entries.put(key, value);
      return this;
    }

    public EndpointAddressResolverMetadata build() {
      return new EndpointAddressResolverMetadata(entries);
    }
  }
}
