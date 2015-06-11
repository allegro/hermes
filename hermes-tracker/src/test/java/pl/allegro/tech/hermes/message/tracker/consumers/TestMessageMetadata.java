package pl.allegro.tech.hermes.message.tracker.consumers;

import java.util.Optional;

public class TestMessageMetadata {

    public static MessageMetadata of(String messageId, String topic, String subscription) {
        return of(messageId, topic, subscription, 1L, 1);
    }

    public static MessageMetadata of(String messageId, String topic, String subscription, long offset, int partition) {
        return new MessageMetadata(messageId, offset, partition, topic, subscription, Optional.of(123456L), Optional.of(123456L));
    }
}
