package pl.allegro.tech.hermes.frontend.publishing;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.frontend.publishing.avro.AvroMessage;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class MessageContentTypeEnforcer {

    private final JsonAvroConverter converter = new JsonAvroConverter();

    private static final String APPLICATION_JSON_WITH_DELIM = APPLICATION_JSON + ";";

    public byte[] enforceAvro(String payloadContentType, byte[] data, Schema schema) {
        if (isJSON(payloadContentType)) {
            return converter.convertToAvro(data, schema);
        }
        return data;
    }

    private boolean isJSON(String contentType) {
        return contentType != null && (contentType.length() > APPLICATION_JSON.length() ?
                contentType.startsWith(APPLICATION_JSON_WITH_DELIM) : contentType.equals(APPLICATION_JSON));
    }
}
