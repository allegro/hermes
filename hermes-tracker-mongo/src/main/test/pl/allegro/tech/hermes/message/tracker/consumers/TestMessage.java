package pl.allegro.tech.hermes.message.tracker.consumers;

import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

import java.util.Optional;

import static java.nio.charset.Charset.defaultCharset;

public class TestMessage {

    public static Message of(String messageId) {
        return of(messageId, 1L, 1);
    }

    public static Message of(String messageId, long offset, int partition) {
        return new Message(Optional.of(messageId), offset, partition, "group.topic", "content".getBytes(defaultCharset()),
                Optional.of(1234567L), Optional.of(12345678L));
    }
}
