package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import pl.allegro.tech.hermes.api.SchemaSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.google.common.io.ByteStreams.toByteArray;
import static org.apache.avro.SchemaBuilder.record;

public class AvroMessageContentWrapper {

    public UnwrappedMessageContent unwrapContent(byte[] data, Schema schema) {
        BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(data, null);

        try {
            return new UnwrappedMessageContent(
                new MessageMetadata(binaryDecoder.readLong(), binaryDecoder.readString()), toByteArray(binaryDecoder.inputStream()));
        } catch (IOException exception) {
            throw new UnwrappingException("Could not read hermes avro message", exception);
        }
    }

    public byte[] wrapContent(byte[] message, String id, long timestamp, Schema schema) {
        try {
            ByteArrayOutputStream wrappedMessage = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(wrappedMessage, null);
            encoder.writeLong(timestamp);
            encoder.writeString(id);
            encoder.writeFixed(message);
            encoder.flush();
            return wrappedMessage.toByteArray();
        } catch (IOException exception) {
            throw new WrappingException("Could not wrap avro message", exception);
        }
    }

    public Schema getWrappedSchema(SchemaSource messageSchema) {
        return getWrappedSchema(new Schema.Parser().parse(messageSchema.value()));
    }

    public Schema getWrappedSchema(Schema messageSchema) {
        return record("MessageWithMetadata")
                .namespace("pl.allegro.tech.hermes")
                .fields()
                    .name("metadata").type().record("MessageMetadata")
                        .fields()
                            .name("timestamp").type().longType().noDefault()
                            .name("id").type().stringType().noDefault()
                        .endRecord().noDefault()
                    .name("message").type(messageSchema).noDefault()
                .endRecord();
    }

}
