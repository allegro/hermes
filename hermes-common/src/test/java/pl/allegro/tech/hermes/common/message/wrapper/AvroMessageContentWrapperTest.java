package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.avro.RecordToBytesConverter.bytesToRecord;

public class AvroMessageContentWrapperTest {
    private Schema schema;
    private AvroMessageContentWrapper avroMessageContentWrapper;
    private AvroUser avroUser;
    private byte[] content;

    private final String id = UUID.randomUUID().toString();
    private final Long timestamp = System.currentTimeMillis();

    @Before
    public void setup() throws IOException {
        avroUser = new AvroUser();
        content = avroUser.create("Bob", 10, "red");
        avroMessageContentWrapper = new AvroMessageContentWrapper();
        schema = avroMessageContentWrapper.getWrappedSchema(avroUser.getSchema());
    }

    @Test
    public void shouldWrapAndUnwrapAvroMessageWithMetadata() throws IOException {
        // when
        byte [] wrappedMessage = avroMessageContentWrapper.wrapContent(content, id, timestamp, null);
        UnwrappedMessageContent unwrappedMessageContent = avroMessageContentWrapper.unwrapContent(wrappedMessage, null);

        // then
        assertThat(unwrappedMessageContent.getMessageMetadata().getId()).isEqualTo(id);
        assertThat(unwrappedMessageContent.getMessageMetadata().getTimestamp()).isEqualTo(timestamp);
        assertThat(unwrappedMessageContent.getContent()).isEqualTo(content);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldWrappedMessageBeValidWithHermesSchema() throws IOException {
        // given
        byte[] wrappedMessage = avroMessageContentWrapper.wrapContent(content, id, timestamp, null);

        // when
        GenericRecord messageWithMetadata = bytesToRecord(wrappedMessage, schema);

        // then
        GenericRecord metadata = getRecord(messageWithMetadata, "metadata");
        assertThat(metadata.get("id").toString()).isEqualTo(id);
        assertThat(metadata.get("timestamp")).isEqualTo(timestamp);
        assertThat(avroUser.userToBytes(getRecord(messageWithMetadata, "message"))).isEqualTo(content);
    }

    private GenericRecord getRecord(GenericRecord rec, String key) {
        return (GenericRecord) rec.get(key);
    }

}