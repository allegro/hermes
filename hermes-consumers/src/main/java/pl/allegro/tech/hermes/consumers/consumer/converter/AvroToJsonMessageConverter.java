package pl.allegro.tech.hermes.consumers.consumer.converter;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import tech.allegro.schema.json2avro.converter.AvroConversionException;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

import javax.inject.Inject;
import java.io.IOException;

import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MARKER;
import static pl.allegro.tech.hermes.consumers.consumer.Message.message;

public class AvroToJsonMessageConverter implements MessageConverter {

    private final JsonAvroConverter converter;

    @Inject
    public AvroToJsonMessageConverter() {
        this.converter = new JsonAvroConverter();
    }

    @Override
    public Message convert(Message message, Topic topic) {
        return message()
                .fromMessage(message)
                .withContentType(ContentType.JSON)
                .withData(converter.convertToJson(recordWithoutMetadata(message.getData(), message.<Schema>getSchema().get().getSchema())))
                .withNoSchema()
                .build();
    }

    private GenericRecord recordWithoutMetadata(byte [] data, Schema schema) {
        GenericRecord original = originalRecord(data, schema);
        Schema schemaWithoutMetadata = removeMetadataField(schema);
        GenericRecordBuilder builder = new GenericRecordBuilder(schemaWithoutMetadata);
        schemaWithoutMetadata.getFields().forEach(field -> builder.set(field, original.get(field.name())));
        return builder.build();
    }

    private Schema removeMetadataField(Schema schema) {
        return Schema.createRecord(
                schema.getFields().stream()
                        .filter(field -> !METADATA_MARKER.equals(field.name()))
                        .map(field -> new Schema.Field(field.name(), field.schema(), field.doc(), field.defaultValue()))
                        .collect(toList()));
    }

    private GenericRecord originalRecord(byte[] data, Schema schema) {
        try {
            BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(data, null);
            return new GenericDatumReader<GenericRecord>(schema).read(null, binaryDecoder);
        } catch (IOException e) {
            throw new AvroConversionException("Failed to create avro record.", e);
        }
    }

}
