package pl.allegro.tech.hermes.message.tracker.consumers;

import java.util.Optional;

public class TestMessageMetadata {

    public static MessageMetadata of(String messageId) {
        return of(messageId, 1L, 1);
    }

    public static MessageMetadata of(String messageId, long offset, int partition) {
        return new MessageMetadata(messageId, offset, partition, "group.topic", Optional.of(1234567L), Optional.of(12345678L));
    }
}
