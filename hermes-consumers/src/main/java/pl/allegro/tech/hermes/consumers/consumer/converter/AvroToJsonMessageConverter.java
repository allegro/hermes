package pl.allegro.tech.hermes.consumers.consumer.converter;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import pl.allegro.tech.common.avro.AvroConversionException;
import pl.allegro.tech.common.avro.JsonAvroConverter;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.converter.schema.AvroSchemaRepositoryMetadataAware;

import javax.inject.Inject;
import java.io.IOException;

import static pl.allegro.tech.hermes.consumers.consumer.Message.message;

public class AvroToJsonMessageConverter implements MessageConverter {

    private final AvroSchemaRepositoryMetadataAware schemaRepository;
    private final JsonAvroConverter converter;

    @Inject
    public AvroToJsonMessageConverter(AvroSchemaRepositoryMetadataAware schemaRepository) {
        this.schemaRepository = schemaRepository;
        this.converter = new JsonAvroConverter();
    }

    @Override
    public Message convert(Message message, Topic topic) {
        return message()
                .fromMessage(message)
                .withContentType(ContentType.JSON)
                .withData(converter.convertToJson(recordWithoutMetadata(message.getData(), topic)))
                .build();
    }

    private GenericRecord recordWithoutMetadata(byte [] data, Topic topic) {
        GenericRecord original = originalRecord(data, topic);
        Schema schemaWithoutMetadata = schemaRepository.getSchemaWithoutMetadata(topic);
        GenericRecordBuilder builder = new GenericRecordBuilder(schemaWithoutMetadata);
        schemaWithoutMetadata.getFields().forEach(field -> builder.set(field, original.get(field.name())));
        return builder.build();
    }

    private GenericRecord originalRecord(byte [] data, Topic topic) {
        try {
            BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(data, null);
            return new GenericDatumReader<GenericRecord>(schemaRepository.getSchema(topic)).read(null, binaryDecoder);
        } catch (IOException e) {
            throw new AvroConversionException("Failed to create avro record.", e);
        }
    }

}
