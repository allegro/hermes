package pl.allegro.tech.hermes.frontend.publishing.message;

import org.apache.avro.Schema;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

public class MessageToJsonConverter {
    private final JsonAvroConverter converter = new JsonAvroConverter();

    public byte[] convert(Message message) {
        try {
            return message.<Schema>getCompiledSchema()
                    .map(schema -> converter.convertToJson(message.getData(), schema.getSchema()))
                    .orElseGet(message::getData);
        } catch (Exception ignored) {
            return message.getData();
        }
    }
}
