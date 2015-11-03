package pl.allegro.tech.hermes.common.message.wrapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.data.MapEntry;
import org.junit.Ignore;
import org.junit.Test;
import pl.allegro.tech.hermes.api.TraceInfo;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class JsonMessageContentWrapperTest {

    private final static byte[] CONTENT = "{\"key\":\"value\"}".getBytes();
    private final static String TRACE_ID = UUID.randomUUID().toString();
    private final ObjectMapper mapper = new ObjectMapper();
    private final MessageMetadata metadata = new MessageMetadata(System.currentTimeMillis(), "14cf17ea-f1ea-a464-6bd6478615bb", TRACE_ID);
    private final Map<String, Object> metadataAsMap = ImmutableMap.of("timestamp", metadata.getTimestamp(), "id", metadata.getId(), "traceId", TRACE_ID);
    private final MapEntry unwrappingMarker = entry("_w", true);
    private final MapEntry content = entry("message", readMap(CONTENT));

    private final JsonMessageContentWrapper contentWrapper = new JsonMessageContentWrapper(content.key.toString(), "metadata", mapper);

    @Test
    @SuppressWarnings("unchecked")
    public void shouldWrapJsonWithMetadata() {
        // given
        TraceInfo traceInfo = new TraceInfo(TRACE_ID);

        //when
        byte[] result = contentWrapper.wrapContent(CONTENT, metadata.getId(), traceInfo, metadata.getTimestamp());

        //then
        assertThat(readMap(result)).containsExactly(unwrappingMarker, entry("metadata", metadataAsMap), content);
    }

    @Test
    public void shouldUnwrapMessageWithMetadata() {
        // given
        TraceInfo traceInfo = new TraceInfo(TRACE_ID);

        //when
        UnwrappedMessageContent result = contentWrapper.unwrapContent(
                contentWrapper.wrapContent(CONTENT, metadata.getId(), traceInfo, metadata.getTimestamp()));

        //then
        assertThat(result.getContent()).isEqualTo(CONTENT);
        assertThat(result.getMessageMetadata()).isEqualTo(metadata);
    }

    @Test
    public void shouldTolerateUnwrappingUnwrappedMessage() {
        //when
        UnwrappedMessageContent result = contentWrapper.unwrapContent(CONTENT);

        //then
        assertThat(result.getMessageMetadata().getId()).isNotEmpty();
        assertThat(result.getMessageMetadata().getTimestamp()).isEqualTo(1L);
    }

    @Ignore
    @Test(expected = UnwrappingException.class)
    public void shouldThrowExceptionWhenMetadataNotFound() {
        contentWrapper.unwrapContent(CONTENT);
    }

    private Map<String, Object> readMap(byte[] result) {
        try {
            return mapper.readValue(new String(result), new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            throw new IllegalStateException("Error while reading map", e);
        }
    }

}
