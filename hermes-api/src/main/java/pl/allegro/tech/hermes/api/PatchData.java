package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.HashMap;
import java.util.Map;
import pl.allegro.tech.hermes.api.jackson.PatchDataDeserializer;
import pl.allegro.tech.hermes.api.jackson.PatchDataSerializer;

@JsonDeserialize(using = PatchDataDeserializer.class)
@JsonSerialize(using = PatchDataSerializer.class)
public class PatchData {

  private final Map<String, Object> patch;

  public PatchData(Map<String, Object> patch) {
    this.patch = patch;
  }

  public static PatchData from(Map<String, Object> patch) {
    return new PatchData(patch);
  }

  public static Builder patchData() {
    return new Builder();
  }

  public Map<String, Object> getPatch() {
    return patch;
  }

  public boolean valueChanged(String field, Object originalValue) {
    return patch.containsKey(field) && !patch.get(field).equals(originalValue);
  }

  public static class Builder {

    private final Map<String, Object> map = new HashMap<>();

    public PatchData build() {
      return PatchData.from(map);
    }

    public Builder set(String field, Object value) {
      this.map.put(field, value);
      return this;
    }
  }
}
