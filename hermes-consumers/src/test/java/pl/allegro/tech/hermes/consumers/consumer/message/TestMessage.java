package pl.allegro.tech.hermes.consumers.consumer.message;

import pl.allegro.tech.hermes.consumers.consumer.Message;

import static java.nio.charset.Charset.defaultCharset;

public class TestMessage {

    public static Message of(String messageId) {
        return of(messageId, 1L, 1);
    }

    public static Message of(String messageId, long offset, int partition) {
        return new Message(messageId, offset, partition, "group.topic", "content".getBytes(defaultCharset()), 1234567L, 12345678L);
    }
}
