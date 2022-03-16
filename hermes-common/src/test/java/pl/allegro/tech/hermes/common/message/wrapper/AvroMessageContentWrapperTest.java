package pl.allegro.tech.hermes.common.message.wrapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.io.IOException;
import java.time.Clock;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static java.lang.Long.valueOf;
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
    public void shouldWrapAndUnwrapAvroMessageWithMetadata() {
        // when
        byte [] wrappedMessage = avroMessageContentWrapper.wrapContent(content, id, timestamp, avroUser.getSchema(), Collections.emptyMap());
        UnwrappedMessageContent unwrappedMessageContent = avroMessageContentWrapper.unwrapContent(wrappedMessage, avroUser.getCompiledSchema());

        // then
        assertThat(unwrappedMessageContent.getMessageMetadata().getId()).isEqualTo(id);
        assertThat(unwrappedMessageContent.getMessageMetadata().getTimestamp()).isEqualTo(timestamp);
        assertThat(unwrappedMessageContent.getContent()).contains(content);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldWrappedMessageContainsMetadata() {
        // when
        byte[] wrappedMessage = avroMessageContentWrapper.wrapContent(content, id, timestamp, avroUser.getSchema(), Collections.emptyMap());

        // then
        GenericRecord messageWithMetadata = bytesToRecord(wrappedMessage, avroUser.getSchema());
        Map<Utf8, Utf8> metadata = (Map<Utf8, Utf8>) messageWithMetadata.get(METADATA_MARKER);
        assertThat(metadata.get(METADATA_MESSAGE_ID_KEY).toString()).isEqualTo(id);
        assertThat(valueOf(metadata.get(METADATA_TIMESTAMP_KEY).toString())).isEqualTo(timestamp);
        assertThat(wrappedMessage).contains(content);
    }

    @Test
    public void shouldWrappedMessageBeUnchangedWhenSchemaNotContainsMetadata() throws IOException {
        // when
        Schema schemaWithoutMetadata = new Schema.Parser().parse(IOUtils.toString(getClass().getResourceAsStream("/schema/user_no_metadata.avsc"), StandardCharsets.UTF_8));
        byte[] wrappedMessage = avroMessageContentWrapper.wrapContent(content, id, timestamp, schemaWithoutMetadata, Collections.emptyMap());

        // then
        GenericRecord wrappedMessageAsRecord = bytesToRecord(wrappedMessage, avroUser.getSchema());
        assertThat(wrappedMessageAsRecord.get(METADATA_MARKER)).isNull();
    }

    @Test
    public void shouldUnwrapAvroMessageAndGenerateMetadataWhenNotExists() throws Throwable {
        //given
        byte [] wrappedMessage = wrapContentWithoutMetadata(content, avroUser.getSchema());

        //when
        UnwrappedMessageContent unwrappedMessage = avroMessageContentWrapper.unwrapContent(wrappedMessage, avroUser.getCompiledSchema());

        //then
        assertThat(unwrappedMessage.getMessageMetadata().getId()).isEmpty();
        assertThat(unwrappedMessage.getMessageMetadata().getTimestamp()).isNotNull();
        assertThat(unwrappedMessage.getContent()).startsWith(content);
    }

    @Test
    public void shouldUnwrapAvroMessageAndSetEmptyMessageIdWhenNotGivenInMetadata() throws Throwable {
        // given
        byte [] wrappedMessage = wrapContentWithoutMessageIdInMetadata(content, avroUser.getSchema());

        //when
        UnwrappedMessageContent unwrappedMessage = avroMessageContentWrapper.unwrapContent(wrappedMessage, avroUser.getCompiledSchema());

        // then
        assertThat(unwrappedMessage.getMessageMetadata().getId()).isEmpty();
        assertThat(unwrappedMessage.getMessageMetadata().getTimestamp()).isNotNull();
        assertThat(unwrappedMessage.getContent()).contains(content);
    }

    private byte[] wrapContentWithoutMetadata(byte[] message, Schema schema) throws Exception {
        return wrapContent(message, schema, null);
    }

    private byte[] wrapContentWithoutMessageIdInMetadata(byte[] message, Schema schema) throws Exception {
        return wrapContent(message, schema, metadataMapWithoutMessageId(timestamp));
    }

    private byte[] wrapContent(byte[] message, Schema schema, Map<Utf8, Utf8> metadata) throws Exception {
        GenericRecord genericRecord = bytesToRecord(message, schema);
        genericRecord.put(METADATA_MARKER, metadata);
        return recordToBytes(genericRecord, schema);
    }

    private Map<Utf8, Utf8> metadataMapWithoutMessageId(long timestamp) {
        Map<Utf8, Utf8> metadata = new HashMap<>();
        metadata.put(METADATA_TIMESTAMP_KEY, new Utf8(Long.toString(timestamp)));
        return metadata;
    }
}
