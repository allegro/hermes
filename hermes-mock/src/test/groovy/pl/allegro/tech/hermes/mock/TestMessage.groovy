package pl.allegro.tech.hermes.mock

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper

class TestMessage {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String key
    private String value

    String getKey() {
        return key
    }

    void setKey(String key) {
        this.key = key
    }

    String getValue() {
        return value
    }

    void setValue(String value) {
        this.value = value
    }

    TestMessage() {}

    TestMessage(String key, Object value) {
        this.key = key
        this.value = value
    }

    static TestMessage random() {
        return new TestMessage("random", UUID.randomUUID().toString());
    }

    String body() {
        return toString();
    }

    @Override
    String toString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
