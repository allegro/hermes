package pl.allegro.tech.hermes.test.helper.message;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public final class TestMessage {

    private final Map<String, String> content = new LinkedHashMap<>();

    private TestMessage() {
    }

    public static TestMessage of(String key, String value) {
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

    public TestMessage append(String key, String value) {
        content.put(key, value);
        return this;
    }

    public String body() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (Entry<String, String> entry : content.entrySet()) {
            builder.append('"').append(entry.getKey()).append("\":").append('"').append(entry.getValue()).append("\",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append("}");
        return builder.toString();
    }

}
