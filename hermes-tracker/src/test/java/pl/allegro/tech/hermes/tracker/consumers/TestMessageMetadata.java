package pl.allegro.tech.hermes.tracker.consumers;

public class TestMessageMetadata {

    public static MessageMetadata of(String messageId, String topic, String subscription) {
        return of(messageId, topic, subscription, 1L, 1);
    }

    public static MessageMetadata of(String messageId, String topic, String subscription, long offset, int partition) {
        return new MessageMetadata(messageId, offset, partition, topic, subscription, 123456L, 123456L);
    }

    public static MessageMetadata of(String messageId,String batchId, String topic, String subscription) {
        return of(messageId, batchId, topic, subscription, 1L, 1);
    }

    public static MessageMetadata of(String messageId, String batchId, String topic, String subscription, long offset, int partition) {
        return new MessageMetadata(messageId, batchId, offset, partition, topic, subscription, 123456L, 123456L);
    }
}
