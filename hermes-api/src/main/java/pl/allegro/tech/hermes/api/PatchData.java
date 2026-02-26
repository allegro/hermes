package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.HashMap;
import java.util.Map;
import pl.allegro.tech.hermes.api.jackson.PatchDataDeserializer;
import pl.allegro.tech.hermes.api.jackson.PatchDataSerializer;

@JsonDeserialize(using = PatchDataDeserializer.class)
@JsonSerialize(using = PatchDataSerializer.class)
public record PatchData(Map<String, Object> patch) {

  public static PatchData from(Map<String, Object> patch) {
    return new PatchData(patch);
  }

  public static PatchData.Builder patchData() {
    return new PatchData.Builder();
  }

  public boolean valueChanged(String field, Object originalValue) {
    return patch.containsKey(field) && !patch.get(field).equals(originalValue);
  }

  public static class Builder {

    private final Map<String, Object> map = new HashMap<>();

    public PatchData build() {
      return PatchData.from(map);
    }

    public PatchData.Builder set(String field, Object value) {
      this.map.put(field, value);
      return this;
    }
  }
}
