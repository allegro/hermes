package pl.allegro.tech.hermes.test.helper.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class TestMessage {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Object> content = new LinkedHashMap<>();

    private TestMessage() {
    }

    public static TestMessage of(String key, Object value) {
        return new TestMessage().append(key, value);
    }

    public static TestMessage[] simpleMessages(int n) {
        TestMessage[] simpleMessages = new TestMessage[n];

        for (int i = 0; i < n; i++) {
            simpleMessages[i] = simple();
        }

        return simpleMessages;
    }

    public static TestMessage simple() {
        return new TestMessage().append("hello", "world");
    }

    public static TestMessage random() {
        return new TestMessage().append("random", UUID.randomUUID().toString());
    }

    public TestMessage append(String key, Object value) {
        content.put(key, value);
        return this;
    }

    public TestMessage withEmptyAvroMetadata() {
        return append(AvroMetadataMarker.METADATA_MARKER, null);
    }

    public String body() {
        return toString();
    }

    public Map<String, Object> getContent() {
        return content;
    }

    @Override
    public String toString() {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
