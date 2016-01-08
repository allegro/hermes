package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.io.IOException;
import java.time.Clock;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static java.lang.Long.valueOf;
import static java.util.Arrays.copyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.bytesToRecord;
import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.recordToBytes;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MARKER;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MESSAGE_ID_KEY;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_TIMESTAMP_KEY;

public class AvroMessageContentWrapperTest {
    private AvroMessageContentWrapper avroMessageContentWrapper;
    private AvroUser avroUser;
    private byte[] content;

    private final String id = UUID.randomUUID().toString();
    private final Long timestamp = System.currentTimeMillis();

    @Before
    public void setup() throws IOException {
        avroUser = new AvroUser("Bob", 10, "red");
        content = avroUser.asBytes();
        avroMessageContentWrapper = new AvroMessageContentWrapper(Clock.systemDefaultZone());
    }

    @Test
    public void shouldWrapAndUnwrapAvroMessageWithMetadata() throws IOException {
        // when
        byte [] wrappedMessage = avroMessageContentWrapper.wrapContent(content, id, timestamp, avroUser.getSchema(), Collections.emptyMap());
        UnwrappedMessageContent unwrappedMessageContent = avroMessageContentWrapper.unwrapContent(wrappedMessage, avroUser.getSchema());

        // then
        assertThat(unwrappedMessageContent.getMessageMetadata().getId()).isEqualTo(id);
        assertThat(unwrappedMessageContent.getMessageMetadata().getTimestamp()).isEqualTo(timestamp);
        assertThat(unwrappedMessageContent.getContent()).startsWith(copyOf(content, content.length - 1));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldWrappedMessageContainsMetadata() throws IOException {
        // when
        byte[] wrappedMessage = avroMessageContentWrapper.wrapContent(content, id, timestamp, avroUser.getSchema(), Collections.emptyMap());

        // then
        GenericRecord messageWithMetadata = bytesToRecord(wrappedMessage, avroUser.getSchema());
        Map<Utf8, Utf8> metadata = (Map<Utf8, Utf8>) messageWithMetadata.get(METADATA_MARKER);
        assertThat(metadata.get(METADATA_MESSAGE_ID_KEY).toString()).isEqualTo(id);
        assertThat(valueOf(metadata.get(METADATA_TIMESTAMP_KEY).toString())).isEqualTo(timestamp);
        assertThat(wrappedMessage).startsWith(copyOf(content, content.length - 1));
    }

    @Test
    public void shouldUnwrapAvroMessageAndGenerateMetadataWhenNotExists() throws Throwable {
        //given
        byte [] wrappedMessage = wrapContentWithoutMetadata(content, avroUser.getSchema());

        //when
        UnwrappedMessageContent unwrappedMessage = avroMessageContentWrapper.unwrapContent(wrappedMessage, avroUser.getSchema());

        //then
        assertThat(unwrappedMessage.getMessageMetadata().getId()).isNotEmpty();
        assertThat(unwrappedMessage.getMessageMetadata().getTimestamp()).isNotNull();
        assertThat(unwrappedMessage.getContent()).startsWith(copyOf(content, content.length - 1));
    }

    private byte[] wrapContentWithoutMetadata(byte[] message, Schema schema) throws Exception{
        GenericRecord genericRecord = bytesToRecord(message, schema);
        genericRecord.put(METADATA_MARKER, null);
        return recordToBytes(genericRecord, schema);
    }
}