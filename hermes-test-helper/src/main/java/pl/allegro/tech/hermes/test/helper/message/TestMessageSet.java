package pl.allegro.tech.hermes.test.helper.message;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class TestMessageSet {

    private final Set<TestMessage> messages = new HashSet<>();

    private TestMessageSet() {
    }

    public static TestMessageSet of(TestMessage... messages) {
        return new TestMessageSet().append(messages);
    }

    public TestMessageSet append(TestMessage... messages) {
        this.messages.addAll(Arrays.asList(messages));
        return this;
    }

    public String body() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (TestMessage message : messages) {
            builder.append(message.body()).append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append("]");

        return builder.toString();
    }
}
