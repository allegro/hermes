package pl.allegro.tech.hermes.api.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.Map;
import pl.allegro.tech.hermes.api.PatchData;

public class PatchDataDeserializer extends JsonDeserializer<PatchData> {

  @Override
  public PatchData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    Map<String, Object> patch =
        p.getCodec().readValue(p, new TypeReference<Map<String, Object>>() {});
    return new PatchData(patch);
  }
}
