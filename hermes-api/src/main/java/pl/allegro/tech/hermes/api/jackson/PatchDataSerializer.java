package pl.allegro.tech.hermes.api.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Map;
import pl.allegro.tech.hermes.api.PatchData;

public class PatchDataSerializer extends JsonSerializer<PatchData> {
  @Override
  public void serialize(PatchData value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException, JsonProcessingException {
    gen.writeStartObject();

    for (Map.Entry<String, Object> entry : value.getPatch().entrySet()) {
      gen.writeObjectField(entry.getKey(), entry.getValue());
    }

    gen.writeEndObject();
  }
}
