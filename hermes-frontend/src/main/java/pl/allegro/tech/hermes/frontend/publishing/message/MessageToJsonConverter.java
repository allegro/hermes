package pl.allegro.tech.hermes.frontend.publishing.message;

import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.bytesToRecord;

import java.util.List;
import org.apache.avro.Conversion;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.common.message.wrapper.SchemaAwareSerDe;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import tech.allegro.schema.json2avro.converter.AvroJsonConverter;
import tech.allegro.schema.json2avro.converter.conversions.DecimalAsStringConversion;

public class MessageToJsonConverter {
  private final List<Conversion<?>> defaultConversions = List.of(DecimalAsStringConversion.INSTANCE);
  private final AvroJsonConverter converter;

    public MessageToJsonConverter() {
        this.converter = new AvroJsonConverter(defaultConversions.toArray(new Conversion[0]));
    }

    public byte[] convert(Message message, boolean schemaIdAwareSerializationEnabled) {
    try {
      return message
          .<Schema>getCompiledSchema()
          .map(
              schema -> convertToJson(message.getData(), schema, schemaIdAwareSerializationEnabled))
          .orElseGet(message::getData);
    } catch (Exception ignored) {
      return message.getData();
    }
  }

  private byte[] convertToJson(
      byte[] avro, CompiledSchema<Schema> schema, boolean schemaIdAwareSerializationEnabled) {
    byte[] schemaAwareAvro =
        schemaIdAwareSerializationEnabled
            ? SchemaAwareSerDe.trimMagicByteAndSchemaVersion(avro)
            : avro;
    return converter.convertToJson(bytesToRecord(schemaAwareAvro, schema.getSchema(), defaultConversions));
  }
}
