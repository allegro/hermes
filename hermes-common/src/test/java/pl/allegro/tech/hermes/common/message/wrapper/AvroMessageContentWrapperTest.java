package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.TraceInfo;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static java.lang.Long.valueOf;
import static java.util.Arrays.copyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.bytesToRecord;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MARKER;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MESSAGE_ID_KEY;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_PARENT_SPAN_ID_KEY;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_SPAN_ID_KEY;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_TIMESTAMP_KEY;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_TRACE_ID_KEY;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_TRACE_REPORTED_KEY;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_TRACE_SAMPLED_KEY;

public class AvroMessageContentWrapperTest {
    private AvroMessageContentWrapper avroMessageContentWrapper;
    private AvroUser avroUser;
    private byte[] content;

    private final String id = UUID.randomUUID().toString();
    private final Long timestamp = System.currentTimeMillis();
    private final TraceInfo traceInfo = new TraceInfo(UUID.randomUUID().toString(),
            UUID.randomUUID().toString(), UUID.randomUUID().toString(), "1", "0");

    @Before
    public void setup() throws IOException {
        avroUser = new AvroUser();
        content = avroUser.create("Bob", 10, "red");
        avroMessageContentWrapper = new AvroMessageContentWrapper();
    }

    @Test
    public void shouldWrapAndUnwrapAvroMessageWithMetadata() throws IOException {
        // when
        byte [] wrappedMessage = avroMessageContentWrapper.wrapContent(content, id, traceInfo, timestamp, avroUser.getSchema());
        UnwrappedMessageContent unwrappedMessageContent = avroMessageContentWrapper.unwrapContent(wrappedMessage, avroUser.getSchema());

        // then
        assertThat(unwrappedMessageContent.getMessageMetadata().getId()).isEqualTo(id);
        assertThat(unwrappedMessageContent.getMessageMetadata().getTimestamp()).isEqualTo(timestamp);
        assertThat(unwrappedMessageContent.getContent()).startsWith(copyOf(content, content.length - 1));
        assertThat(unwrappedMessageContent.getMessageMetadata().getTraceId()).isEqualTo(traceInfo.getTraceId());
        assertThat(unwrappedMessageContent.getMessageMetadata().getSpanId()).isEqualTo(traceInfo.getSpanId());
        assertThat(unwrappedMessageContent.getMessageMetadata().getParentSpanId()).isEqualTo(traceInfo.getParentSpanId());
        assertThat(unwrappedMessageContent.getMessageMetadata().getTraceSampled()).isEqualTo(traceInfo.getTraceSampled());
        assertThat(unwrappedMessageContent.getMessageMetadata().getTraceReported()).isEqualTo(traceInfo.getTraceReported());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldWrappedMessageContainsMetadata() throws IOException {
        // when
        byte[] wrappedMessage = avroMessageContentWrapper.wrapContent(content, id, traceInfo, timestamp, avroUser.getSchema());

        // then
        GenericRecord messageWithMetadata = bytesToRecord(wrappedMessage, avroUser.getSchema());
        Map<Utf8, Utf8> metadata = (Map<Utf8, Utf8>) messageWithMetadata.get(METADATA_MARKER);
        assertThat(metadata.get(METADATA_MESSAGE_ID_KEY).toString()).isEqualTo(id);
        assertThat(valueOf(metadata.get(METADATA_TIMESTAMP_KEY).toString())).isEqualTo(timestamp);
        assertThat(wrappedMessage).startsWith(copyOf(content, content.length - 1));
        assertThat(metadata.get(METADATA_TRACE_ID_KEY).toString()).isEqualTo(traceInfo.getTraceId());
        assertThat(metadata.get(METADATA_SPAN_ID_KEY).toString()).isEqualTo(traceInfo.getSpanId());
        assertThat(metadata.get(METADATA_PARENT_SPAN_ID_KEY).toString()).isEqualTo(traceInfo.getParentSpanId());
        assertThat(metadata.get(METADATA_TRACE_SAMPLED_KEY).toString()).isEqualTo(traceInfo.getTraceSampled());
        assertThat(metadata.get(METADATA_TRACE_REPORTED_KEY).toString()).isEqualTo(traceInfo.getTraceReported());
    }

}