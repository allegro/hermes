package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.avro.RecordToBytesConverter.bytesToRecord;

public class AvroMessageContentWrapperTest {

    private Schema schema;
    private AvroMessageContentWrapper avroMessageContentWrapper;

    @Before
    public void setup() throws IOException {
        avroMessageContentWrapper = new AvroMessageContentWrapper();
        schema = new Schema.Parser().parse(getClass().getResourceAsStream("/hermesMessage.avsc"));
    }

    @Test
    public void shouldWrapAndUnwrapAvroMessageWithMetadata() throws IOException {
        // given
        String id = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        byte [] content = "message".getBytes();

        // when
        byte [] wrappedMessage = avroMessageContentWrapper.wrapContent(content, id, timestamp);
        UnwrappedMessageContent unwrappedMessageContent = avroMessageContentWrapper.unwrapContent(wrappedMessage);

        // then
        assertThat(unwrappedMessageContent.getMessageMetadata().getId()).isEqualTo(id);
        assertThat(unwrappedMessageContent.getMessageMetadata().getTimestamp()).isEqualTo(timestamp);
        assertThat(unwrappedMessageContent.getContent()).isEqualTo(content);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldWrappedMessageBeValidWithHermesSchema() throws IOException {
        // given
        String id = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        byte [] content = "message".getBytes();
        byte [] wrappedMessage = avroMessageContentWrapper.wrapContent(content, id, timestamp);

        // when
        GenericRecord messageWithMetadata = bytesToRecord(wrappedMessage, schema);

        // then
        GenericRecord metadata = (GenericRecord) messageWithMetadata.get("metadata");
        assertThat(metadata.get("id").toString()).isEqualTo(id);
        assertThat(metadata.get("timestamp")).isEqualTo(timestamp);
        assertThat(((ByteBuffer) messageWithMetadata.get("message")).array()).isEqualTo(content);
    }

}