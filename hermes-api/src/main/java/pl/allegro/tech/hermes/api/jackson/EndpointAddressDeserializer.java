package pl.allegro.tech.hermes.api.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import pl.allegro.tech.hermes.api.EndpointAddress;

public class EndpointAddressDeserializer extends JsonDeserializer<EndpointAddress> {

  @Override
  public EndpointAddress deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    JsonNode node = jp.getCodec().readTree(jp);
    return new EndpointAddress(node.asText());
  }
}
