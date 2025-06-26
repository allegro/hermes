package pl.allegro.tech.hermes.consumers.consumer.converter;

import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.bytesToRecord;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MARKER;
import static pl.allegro.tech.hermes.consumers.consumer.Message.message;

import java.util.List;
import org.apache.avro.Conversion;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import tech.allegro.schema.json2avro.converter.AvroJsonConverter;
import tech.allegro.schema.json2avro.converter.conversions.DecimalAsStringConversion;

public class AvroToJsonMessageConverter implements MessageConverter {

  private final AvroJsonConverter converter;
  private final List<Conversion<?>> defaultConversions =
      List.of(DecimalAsStringConversion.INSTANCE);

  public AvroToJsonMessageConverter() {
    this.converter = new AvroJsonConverter(defaultConversions.toArray(new Conversion<?>[0]));
  }

  @Override
  public Message convert(Message message, Topic topic) {
    return message()
        .fromMessage(message)
        .withContentType(ContentType.JSON)
        .withData(
            converter.convertToJson(
                recordWithoutMetadata(message.getData(), message.getSchema().get().getSchema())))
        .withNoSchema()
        .build();
  }

  private GenericRecord recordWithoutMetadata(byte[] data, Schema schema) {
    GenericRecord original = bytesToRecord(data, schema, defaultConversions);
    Schema schemaWithoutMetadata = removeMetadataField(schema);
    GenericRecordBuilder builder = new GenericRecordBuilder(schemaWithoutMetadata);
    schemaWithoutMetadata
        .getFields()
        .forEach(field -> builder.set(field, original.get(field.name())));
    return builder.build();
  }

  private Schema removeMetadataField(Schema schema) {
    return Schema.createRecord(
        schema.getFields().stream()
            .filter(field -> !METADATA_MARKER.equals(field.name()))
            .map(
                field ->
                    new Schema.Field(field.name(), field.schema(), field.doc(), field.defaultVal()))
            .collect(toList()));
  }
}
