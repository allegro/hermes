package pl.allegro.tech.hermes.mock

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper

class TestMessage {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    String key
    String value

    TestMessage() {}

    TestMessage(String key, Object value) {
        this.key = key
        this.value = value
    }

    static TestMessage random() {
        return new TestMessage("random", UUID.randomUUID().toString());
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
