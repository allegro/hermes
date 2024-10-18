package pl.allegro.tech.hermes.api.jackson;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

public class OptionalInstantIsoSerializer extends JsonSerializer<Optional<Instant>> {

  @Override
  public void serialize(Optional<Instant> value, JsonGenerator jgen, SerializerProvider provider)
      throws IOException {
    if (value.isPresent()) {
      jgen.writeString(ISO_INSTANT.format(value.get()));
    } else {
      jgen.writeNull();
    }
  }
}
