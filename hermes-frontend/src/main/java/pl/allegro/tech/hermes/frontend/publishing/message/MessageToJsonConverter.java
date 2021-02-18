package pl.allegro.tech.hermes.frontend.publishing.message;

import org.apache.avro.Schema;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.bytesToRecord;

public class MessageToJsonConverter {
    private final JsonAvroConverter converter = new JsonAvroConverter();

    public byte[] convert(Message message) {
        try {
            return message.<Schema>getCompiledSchema()
                    .map(schema -> convertToJson(message.getData(), schema.getSchema()))
                    .orElseGet(message::getData);
        } catch (Exception ignored) {
            return message.getData();
        }
    }

    private byte[] convertToJson(byte[] avro, Schema schema) {
        return converter.convertToJson(bytesToRecord(avro, schema));
    }

}
