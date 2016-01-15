package pl.allegro.tech.hermes.frontend.validator;

import com.google.common.collect.ImmutableList;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import javax.inject.Inject;

public class AvroTopicMessageValidator implements TopicMessageValidator {

    private final SchemaRepository<Schema> schemaRepository;

    @Inject
    public AvroTopicMessageValidator(SchemaRepository<Schema> schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    @Override
    public void check(byte[] message, Topic topic) {
        if (ContentType.AVRO != topic.getContentType()) {
            return;
        }

        Schema schema = schemaRepository.getSchema(topic);
        BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(message, null);
        try {
            new GenericDatumReader<>(schema).read(null, binaryDecoder);
        } catch (Exception e) {
            throw new InvalidMessageException("Could not deserialize avro message with provided schema", ImmutableList.of(e.getMessage()));
        }
    }
}
