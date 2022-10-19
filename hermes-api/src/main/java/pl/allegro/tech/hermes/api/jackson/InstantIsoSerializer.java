package pl.allegro.tech.hermes.api.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

public class InstantIsoSerializer extends JsonSerializer<Instant> {

    @Override
    public void serialize(Instant value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeString(ISO_INSTANT.format(value));
    }
}
