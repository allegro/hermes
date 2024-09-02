package pl.allegro.tech.hermes.api.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import pl.allegro.tech.hermes.api.EndpointAddress;

import java.io.IOException;

public class EndpointAddressSerializer extends JsonSerializer<EndpointAddress> {
    @Override
    public void serialize(EndpointAddress value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {
        jgen.writeString(value.getRawEndpoint());
    }
}
