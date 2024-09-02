package pl.allegro.tech.hermes.api.helpers;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.HashMap;
import java.util.Map;
import pl.allegro.tech.hermes.api.PatchData;

public class Patch {

  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .disable(SerializationFeature.WRITE_NULL_MAP_VALUES)
          .registerModule(new JavaTimeModule());

  @SuppressWarnings("unchecked")
  public static <T> T apply(T object, PatchData patch) {
    checkNotNull(object);
    checkNotNull(patch);
    Map objectMap = MAPPER.convertValue(object, Map.class);
    return (T) MAPPER.convertValue(merge(objectMap, patch.getPatch()), object.getClass());
  }

  @SuppressWarnings("unchecked")
  private static Map merge(Map<String, Object> left, Map<String, Object> right) {
    Map<String, Object> merged = new HashMap<>(left);
    for (Map.Entry<String, Object> entry : right.entrySet()) {
      if (entry.getValue() instanceof Map && merged.containsKey(entry.getKey())) {
        Map<String, Object> nested = (Map) merged.get(entry.getKey());
        nested.putAll(merge(nested, (Map) entry.getValue()));
      } else {
        merged.put(entry.getKey(), entry.getValue());
      }
    }
    return merged;
  }
}
