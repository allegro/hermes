package pl.allegro.tech.hermes.frontend.publishing.message;

import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.bytesToRecord;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

public class MessageToJsonConverter {
  private final JsonAvroConverter converter = new JsonAvroConverter();

  public byte[] convert(Message message) {
    try {
      return message
          .<Schema>getCompiledSchema()
          .map(schema -> convertToJson(message.getData(), schema))
          .orElseGet(message::getData);
    } catch (Exception ignored) {
      return message.getData();
    }
  }

  private byte[] convertToJson(byte[] avro, CompiledSchema<Schema> schema) {
    return converter.convertToJson(bytesToRecord(avro, schema.getSchema()));
  }
}
