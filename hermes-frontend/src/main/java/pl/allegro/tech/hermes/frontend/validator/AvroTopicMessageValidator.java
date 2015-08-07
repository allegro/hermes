package pl.allegro.tech.hermes.frontend.validator;

import com.google.common.collect.ImmutableList;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;

public class AvroTopicMessageValidator implements TopicMessageValidator {

    private final GenericDatumReader<GenericRecord> validator;

    public AvroTopicMessageValidator(Schema schema) {
        validator = new GenericDatumReader<>(schema);
    }

    @Override
    public void check(byte[] message) {
        BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(message, null);
        try {
            validator.read(null, binaryDecoder);
        } catch (Exception e) {
            throw new InvalidMessageException("Could not deserialize avro message with provided schema", ImmutableList.of(e.getMessage()));
        }
    }
}
